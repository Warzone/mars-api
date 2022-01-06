package network.warzone.api.socket

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import network.warzone.api.database.Database
import network.warzone.api.database.MatchCache
import network.warzone.api.database.PlayerCache
import network.warzone.api.database.models.FirstBlood
import network.warzone.api.database.models.Participant
import network.warzone.api.database.models.Player
import network.warzone.api.database.models.SimpleParticipant
import network.warzone.api.socket.match.MatchEndData
import network.warzone.api.socket.match.MatchPhaseListener
import network.warzone.api.socket.match.MatchStartData
import network.warzone.api.socket.objective.*
import network.warzone.api.socket.participant.ParticipantContext
import network.warzone.api.socket.participant.ParticipantPartyListener
import network.warzone.api.socket.participant.ParticipantStatListener
import network.warzone.api.socket.player.*
import network.warzone.api.socket.server.MatchLoadData
import network.warzone.api.socket.server.ServerContext
import org.litote.kmongo.eq
import org.litote.kmongo.replaceOne
import redis.clients.jedis.params.SetParams
import java.util.*

class SocketRouter(val server: ServerContext) {
    suspend fun route(event: EventType, data: JsonObject) {
        when (event) {
            EventType.MATCH_LOAD -> onMatchLoad(Json.decodeFromJsonElement(data))
            EventType.MATCH_START -> onMatchStart(Json.decodeFromJsonElement(data))
            EventType.MATCH_END -> onMatchEnd(Json.decodeFromJsonElement(data))
            EventType.PLAYER_DEATH -> onPlayerDeath(Json.decodeFromJsonElement(data))
            EventType.PLAYER_CHAT -> onPlayerChat(Json.decodeFromJsonElement(data))
            EventType.KILLSTREAK -> onKillstreak(Json.decodeFromJsonElement(data))
            EventType.PARTY_JOIN -> onPartyJoin(Json.decodeFromJsonElement(data))
            EventType.PARTY_LEAVE -> onPartyLeave(Json.decodeFromJsonElement(data))
            EventType.DESTROYABLE_DESTROY -> onDestroyableDestroy(Json.decodeFromJsonElement(data))
            EventType.CORE_LEAK -> onCoreLeak(Json.decodeFromJsonElement(data))
            EventType.FLAG_CAPTURE -> onFlagPlace(Json.decodeFromJsonElement(data))
            EventType.FLAG_PICKUP -> onFlagPickup(Json.decodeFromJsonElement(data))
            EventType.FLAG_DROP -> onFlagDrop(Json.decodeFromJsonElement(data))
            EventType.FLAG_DEFEND -> onFlagDefend(Json.decodeFromJsonElement(data))
            EventType.WOOL_CAPTURE -> onWoolPlace(Json.decodeFromJsonElement(data))
            EventType.WOOL_PICKUP -> onWoolPickup(Json.decodeFromJsonElement(data))
            EventType.WOOL_DROP -> onWoolDrop(Json.decodeFromJsonElement(data))
            EventType.WOOL_DEFEND -> onWoolDefend(Json.decodeFromJsonElement(data))
            EventType.CONTROL_POINT_CAPTURE -> onControlPointCapture(Json.decodeFromJsonElement(data))
            else -> println("Event (srv ${server.id}) fell through router: $event - $data")
        }
    }

    private suspend fun onMatchLoad(data: MatchLoadData) {
        MatchPhaseListener(server).onLoad(data)
    }

    private suspend fun onMatchStart(data: MatchStartData) {
        var match = server.match ?: return
        match = MatchPhaseListener(server).onStart(data, match) ?: return
        MatchCache.set(match._id, match)
    }

    private suspend fun onMatchEnd(data: MatchEndData) {
        var match = server.match ?: return
        match = MatchPhaseListener(server).onEnd(data, match) ?: return

        val participantListeners = getParticipantListeners()
        val playerListeners = getPlayerListeners()

        val profiles = mutableListOf<Player>()

        match.participants.forEach { (id, participant) ->
            runBlocking {
                val bigStats = data.bigStats[id] ?: MatchEndData.BigStats()

                var participantContext = ParticipantContext(participant, match)
                participantListeners.forEach {
                    participantContext =
                        it.onMatchEnd(participantContext, data, bigStats, participantContext.getMatchResult(data))
                }
                match.saveParticipants(participantContext.profile)

                var playerContext = participantContext.getPlayerContext()
                playerListeners.forEach {
                    playerContext = it.onMatchEnd(playerContext, data, bigStats, participantContext.getMatchResult(data))
                }
                participant.setPlayer(playerContext.profile)
                profiles.add(playerContext.profile)
            }
        }

        // Write all updated player profiles (since the last match ended) to the database
        // Only players who participated in the match at any point. Players who observed the entire match will not be affected.
        Database.players.bulkWrite(profiles.map { replaceOne(Player::_id eq it._id, it) })

        MatchCache.set(match._id, match, true, SetParams().px(3600000L)) // cache expires one hour after end
    }

    private suspend fun onPlayerDeath(data: PlayerDeathData) {
        val match = server.match ?: throw RuntimeException("Player death fired during no match") // todo: force cycle?

        // Save first blood status & set first blood data if kill is first blood
        val isFirstBlood = match.firstBlood == null && data.isMurder
        if (isFirstBlood) match.firstBlood =
            FirstBlood(data.simpleAttacker!!, data.simpleVictim, Date().time)

        // Fire kill event for attacker
        if (data.isMurder) {
            val attacker = match.participants[data.attackerId]!!

            var participantContext = ParticipantContext(attacker, match)
            getParticipantListeners().forEach { participantContext = it.onKill(participantContext, data, isFirstBlood) }
            match.saveParticipants(participantContext.profile)

            var playerContext = participantContext.getPlayerContext()
            getPlayerListeners().forEach { playerContext = it.onKill(playerContext, data, isFirstBlood) }
            attacker.setPlayer(playerContext.profile)
        }

        // Fire death event for victim
        val victim = match.participants[data.victimId]!!

        var participantContext = ParticipantContext(victim, match)
        getParticipantListeners().forEach { participantContext = it.onDeath(participantContext, data, isFirstBlood) }
        match.saveParticipants(participantContext.profile)

        var playerContext = participantContext.getPlayerContext()
        getPlayerListeners().forEach { playerContext = it.onDeath(playerContext, data, isFirstBlood) }
        victim.setPlayer(playerContext.profile)

        // Save
        MatchCache.set(match._id, match)
    }

    private suspend fun onPlayerChat(data: PlayerChatData) {
        val match = server.match ?: throw RuntimeException("Player chat fired during no match") // todo: force cycle?
        val participant = match.participants[data.playerId]

        if (participant != null) {
            var participantContext = ParticipantContext(participant, match)
            getParticipantListeners().forEach { participantContext = it.onChat(participantContext, data) }
            match.saveParticipants(participantContext.profile)
        }

        val player: Player = PlayerCache.get(data.playerName) ?: return
        var playerContext = PlayerContext(player, match)
        getPlayerListeners().forEach { playerContext = it.onChat(playerContext, data) }
        PlayerCache.set(player.name, playerContext.profile)

        MatchCache.set(match._id, match)
    }

    private suspend fun onKillstreak(data: KillstreakData) {
        val match =
            server.match ?: throw RuntimeException("Killstreak event fired during no match") // todo: force cycle?
        val participant = match.participants[data.player.id]!!

        var participantContext = ParticipantContext(participant, match)
        getParticipantListeners().forEach { participantContext = it.onKillstreak(participantContext, data.amount) }
        match.saveParticipants(participantContext.profile)

        var playerContext = participantContext.getPlayerContext()
        getPlayerListeners().forEach { playerContext = it.onKillstreak(playerContext, data.amount) }
        participant.setPlayer(playerContext.profile)

        MatchCache.set(match._id, match)
    }

    private suspend fun onPartyJoin(data: PartyJoinData) {
        val match = server.match ?: throw RuntimeException("Party join fired during no match") // todo: force cycle?
        val participant = match.participants[data.playerId] ?: Participant(
            SimpleParticipant(
                data.playerName,
                data.playerId,
                data.partyName
            )
        )

        var participantContext = ParticipantContext(participant, match)
        getParticipantListeners().forEach { participantContext = it.onPartyJoin(participantContext, data.partyName) }
        match.saveParticipants(participantContext.profile)

        var playerContext = participantContext.getPlayerContext()
        getPlayerListeners().forEach { playerContext = it.onPartyJoin(playerContext, data.partyName) }
        participant.setPlayer(playerContext.profile)

        MatchCache.set(match._id, match)
    }

    private suspend fun onPartyLeave(data: PartyLeaveData) {
        val match = server.match ?: throw RuntimeException("Party leave fired during no match") // todo: force cycle?
        val participant = match.participants[data.playerId]!!

        var participantContext = ParticipantContext(participant, match)
        getParticipantListeners().forEach { participantContext = it.onPartyLeave(participantContext) }
        match.saveParticipants(participantContext.profile)

        var playerContext = participantContext.getPlayerContext()
        getPlayerListeners().forEach { playerContext = it.onPartyLeave(playerContext) }
        participant.setPlayer(playerContext.profile)

        match.saveParticipants(participantContext.profile)
        MatchCache.set(match._id, match)
    }

    private suspend fun onDestroyableDestroy(data: DestroyableDestroyData) {
        val match =
            server.match ?: throw RuntimeException("Destroyable destroy fired during no match") // todo: force cycle?

        val participantListeners = getParticipantListeners()
        val playerListeners = getPlayerListeners()

        data.contributions.forEach { contribution ->
            val participant = match.participants[contribution.playerId] ?: return

            var participantContext = ParticipantContext(participant, match)
            participantListeners.forEach {
                participantContext =
                    it.onDestroyableDestroy(participantContext, contribution.percentage, contribution.blockCount)
            }
            match.saveParticipants(participantContext.profile)

            var playerContext = participantContext.getPlayerContext()
            playerListeners.forEach {
                playerContext = it.onDestroyableDestroy(playerContext, contribution.percentage, contribution.blockCount)
            }
            participant.setPlayer(playerContext.profile)
        }

        MatchCache.set(match._id, match)
    }

    private suspend fun onCoreLeak(data: CoreLeakData) {
        val match = server.match ?: throw RuntimeException("Core leak fired during no match") // todo: force cycle?

        val participantListeners = getParticipantListeners()
        val playerListeners = getPlayerListeners()

        data.contributions.forEach { contribution ->
            val participant = match.participants[contribution.playerId] ?: return

            var participantContext = ParticipantContext(participant, match)
            participantListeners.forEach {
                participantContext = it.onCoreLeak(participantContext, contribution.percentage, contribution.blockCount)
            }
            match.saveParticipants(participantContext.profile)

            var playerContext = participantContext.getPlayerContext()
            playerListeners.forEach {
                playerContext = it.onCoreLeak(playerContext, contribution.percentage, contribution.blockCount)
            }
            participant.setPlayer(playerContext.profile)
        }

        MatchCache.set(match._id, match)
    }

    private suspend fun onFlagPlace(data: FlagDropData) {
        val match = server.match ?: throw RuntimeException("Flag place fired during no match") // todo: force cycle?
        val participant = match.participants[data.playerId]!!

        var participantContext = ParticipantContext(participant, match)
        getParticipantListeners().forEach { participantContext = it.onFlagPlace(participantContext, data.heldTime) }

        var playerContext = participantContext.getPlayerContext()
        getPlayerListeners().forEach { playerContext = it.onFlagPlace(playerContext, data.heldTime) }
        participant.setPlayer(playerContext.profile)

        match.saveParticipants(participantContext.profile)
        MatchCache.set(match._id, match)
    }

    private suspend fun onFlagPickup(data: FlagEventData) {
        val match = server.match ?: throw RuntimeException("Flag pickup fired during no match") // todo: force cycle?
        val participant = match.participants[data.playerId]!!

        var participantContext = ParticipantContext(participant, match)
        getParticipantListeners().forEach { participantContext = it.onFlagPickup(participantContext) }
        match.saveParticipants(participantContext.profile)

        var playerContext = participantContext.getPlayerContext()
        getPlayerListeners().forEach { playerContext = it.onFlagPickup(playerContext) }
        participant.setPlayer(playerContext.profile)

        MatchCache.set(match._id, match)
    }

    private suspend fun onFlagDrop(data: FlagDropData) {
        val match = server.match ?: throw RuntimeException("Flag drop fired during no match") // todo: force cycle?
        val participant = match.participants[data.playerId]!!

        var participantContext = ParticipantContext(participant, match)
        getParticipantListeners().forEach { participantContext = it.onFlagDrop(participantContext, data.heldTime) }
        match.saveParticipants(participantContext.profile)

        var playerContext = participantContext.getPlayerContext()
        getPlayerListeners().forEach { playerContext = it.onFlagDrop(playerContext, data.heldTime) }
        participant.setPlayer(playerContext.profile)

        MatchCache.set(match._id, match)
    }

    private suspend fun onFlagDefend(data: FlagEventData) {
        val match = server.match ?: throw RuntimeException("Flag defend fired during no match") // todo: force cycle?
        val participant = match.participants[data.playerId]!!

        var participantContext = ParticipantContext(participant, match)
        getParticipantListeners().forEach { participantContext = it.onFlagDefend(participantContext) }
        match.saveParticipants(participantContext.profile)

        var playerContext = participantContext.getPlayerContext()
        getPlayerListeners().forEach { playerContext = it.onFlagDefend(playerContext) }
        participant.setPlayer(playerContext.profile)

        MatchCache.set(match._id, match)
    }

    private suspend fun onWoolPlace(data: WoolDropData) {
        val match = server.match ?: throw RuntimeException("Wool place fired during no match") // todo: force cycle?
        val participant = match.participants[data.playerId]!!

        var participantContext = ParticipantContext(participant, match)
        getParticipantListeners().forEach { participantContext = it.onWoolPlace(participantContext, data.heldTime) }
        match.saveParticipants(participantContext.profile)

        var playerContext = participantContext.getPlayerContext()
        getPlayerListeners().forEach { playerContext = it.onWoolPlace(playerContext, data.heldTime) }
        participant.setPlayer(playerContext.profile)

        MatchCache.set(match._id, match)
    }

    private suspend fun onWoolPickup(data: WoolEventData) {
        val match = server.match ?: throw RuntimeException("Wool pickup fired during no match") // todo: force cycle?
        val participant = match.participants[data.playerId]!!

        var participantContext = ParticipantContext(participant, match)
        getParticipantListeners().forEach { participantContext = it.onWoolPickup(participantContext) }
        match.saveParticipants(participantContext.profile)

        var playerContext = participantContext.getPlayerContext()
        getPlayerListeners().forEach { playerContext = it.onWoolPickup(playerContext) }
        participant.setPlayer(playerContext.profile)

        MatchCache.set(match._id, match)
    }

    private suspend fun onWoolDrop(data: WoolDropData) {
        val match = server.match ?: throw RuntimeException("Wool drop fired during no match") // todo: force cycle?
        val participant = match.participants[data.playerId]!!

        var participantContext = ParticipantContext(participant, match)
        getParticipantListeners().forEach { participantContext = it.onWoolDrop(participantContext, data.heldTime) }
        match.saveParticipants(participantContext.profile)

        var playerContext = participantContext.getPlayerContext()
        getPlayerListeners().forEach { playerContext = it.onWoolDrop(playerContext, data.heldTime) }
        participant.setPlayer(playerContext.profile)

        MatchCache.set(match._id, match)
    }

    private suspend fun onWoolDefend(data: WoolEventData) {
        val match = server.match ?: throw RuntimeException("Wool defend fired during no match") // todo: force cycle?
        val participant = match.participants[data.playerId]!!

        var participantContext = ParticipantContext(participant, match)
        getParticipantListeners().forEach { participantContext = it.onWoolDefend(participantContext) }
        match.saveParticipants(participantContext.profile)

        var playerContext = participantContext.getPlayerContext()
        getPlayerListeners().forEach { playerContext = it.onWoolDefend(playerContext) }
        participant.setPlayer(playerContext.profile)

        MatchCache.set(match._id, match)
    }

    private suspend fun onControlPointCapture(data: ControlPointCaptureData) {
        val match = server.match
            ?: throw RuntimeException("Control point capture fired during no match") // todo: force cycle? - catch special exception?

        val participantListeners = getParticipantListeners()
        val playerListeners = getPlayerListeners()

        data.playerIds.forEach { id ->
            val participant = match.participants[id] ?: return

            var participantContext = ParticipantContext(participant, match)
            participantListeners.forEach {
                participantContext = it.onControlPointCapture(participantContext, data.playerIds.size)
            }
            match.saveParticipants(participantContext.profile)

            var playerContext = participantContext.getPlayerContext()
            playerListeners.forEach { playerContext = it.onControlPointCapture(playerContext, data.playerIds.size) }
            participant.setPlayer(playerContext.profile)
        }

        MatchCache.set(match._id, match)
    }
}

fun getParticipantListeners(): List<PlayerListener<ParticipantContext>> {
    return listOf(ParticipantStatListener(), ParticipantPartyListener())
}

fun getPlayerListeners(): List<PlayerListener<PlayerContext>> {
    return listOf(PlayerStatListener(), PlayerGamemodeStatListener(), PlayerXPListener, PlayerRecordListener)
}