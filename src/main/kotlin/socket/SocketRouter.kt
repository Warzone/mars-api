package network.warzone.api.socket

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import network.warzone.api.database.Database
import network.warzone.api.database.MatchCache
import network.warzone.api.database.PlayerCache
import network.warzone.api.database.Redis
import network.warzone.api.database.models.*
import network.warzone.api.socket.achievement.PlayerUpdateListener
import network.warzone.api.socket.leaderboard.LeaderboardListener
import network.warzone.api.socket.map.MapRecordListener
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
import network.warzone.api.util.WebhookUtil
import org.litote.kmongo.eq
import org.litote.kmongo.replaceOne
import redis.clients.jedis.params.SetParams
import java.util.*


class SocketRouter(val server: ServerContext) {
    private val participantListeners =
        listOf(ParticipantStatListener, ParticipantPartyListener, MapRecordListener, LeaderboardListener)
    private val playerListeners =
        listOf(PlayerStatListener, PlayerGamemodeStatListener, PlayerXPListener, PlayerRecordListener, PlayerUpdateListener(server))
    suspend fun route(event: EventType, data: JsonObject) {
        try {
            when (event) {
                EventType.ACHIEVEMENT_EARN -> onAchievementComplete(Json.decodeFromJsonElement(data))
                EventType.MATCH_LOAD -> onMatchLoad(Json.decodeFromJsonElement(data))
                EventType.MATCH_START -> onMatchStart(Json.decodeFromJsonElement(data))
                EventType.MATCH_END -> onMatchEnd(Json.decodeFromJsonElement(data))
                EventType.PLAYER_DEATH -> onPlayerDeath(Json.decodeFromJsonElement(data))
                EventType.PLAYER_CHAT -> onPlayerChat(Json.decodeFromJsonElement(data))
                EventType.KILLSTREAK -> onKillstreak(Json.decodeFromJsonElement(data))
                EventType.PARTY_JOIN -> onPartyJoin(Json.decodeFromJsonElement(data))
                EventType.PARTY_LEAVE -> onPartyLeave(Json.decodeFromJsonElement(data))
                EventType.DESTROYABLE_DESTROY -> onDestroyableDestroy(Json.decodeFromJsonElement(data))
                EventType.DESTROYABLE_DAMAGE -> onDestroyableDamage(Json.decodeFromJsonElement(data))
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
                else -> logger.warn("Event (srv ${server.id}) fell through router: $event - $data")
            }
        } catch (e: InvalidMatchStateException) {
            // Automatically end the match and force Mars to load a new match to sync state
            server.call(EventType.FORCE_MATCH_END, Unit)
            logger.warn("Forcing match end for Match ID: ${server.match?._id}. Caused by ${event.name}: ${e.message}")
            WebhookUtil.sendDebugLogWebhook("(${server.id}) INVALID MATCH STATE. Last known match ID: ${server.match?._id} - Map: ${server.match?.level?.name} (`${server.match?.level?._id}`). Caused by event ${event.name}. Stats are not being tracked as a result of this. Ending match is recommended.")
        } catch (ex: Exception) {
            logger.warn("Exception occurred (match id: ${server.match?._id}) – caused by ${event.name}: $ex ${ex.message}")
            ex.printStackTrace()
            WebhookUtil.sendDebugLogWebhook("[${server.id}, ${server.match?._id}, ${server.match?.level?.name}] Exception occurred (SR) $ex ${ex.message}")
        }
    }

    private suspend fun onMatchLoad(data: MatchLoadData) {
        MatchPhaseListener(server).onLoad(data)
    }

    private suspend fun onMatchStart(data: MatchStartData) {
        var match = server.match ?: throw InvalidMatchStateException()
        if (match.state != MatchState.PRE) throw InvalidMatchStateException()
        match = MatchPhaseListener(server).onStart(data, match)
        MatchCache.set(match._id, match)
    }

    private suspend fun onMatchEnd(data: MatchEndData) {
        var match = server.match ?: throw InvalidMatchStateException()
        if (match.state != MatchState.IN_PROGRESS) throw InvalidMatchStateException()
        match = MatchPhaseListener(server).onEnd(data, match)

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
                    playerContext =
                        it.onMatchEnd(playerContext, data, bigStats, participantContext.getMatchResult(data))
                }
                participant.setPlayer(playerContext.profile)
                profiles.add(playerContext.profile)
            }
        }

        // Write all updated player profiles (since the last match ended) to the database
        // Only players who participated in the match at any point. Players who observed the entire match will not be affected.
        if (profiles.isNotEmpty()) Database.players.bulkWrite(profiles.map { replaceOne(Player::_id eq it._id, it) })

        Database.levels.save(match.level)
        MatchCache.set(match._id, match, true, SetParams().px(3600000L)) // cache expires one hour after end
    }

    private suspend fun onPlayerDeath(data: PlayerDeathData) {
        val match = server.match ?: throw InvalidMatchStateException()
        if (match.state != MatchState.IN_PROGRESS) throw InvalidMatchStateException()

        // Save first blood status & set first blood data if kill is first blood
        val isFirstBlood = match.firstBlood == null && data.isMurder
        if (isFirstBlood) match.firstBlood =
            FirstBlood(data.attacker!!, data.victim, Date().time)

        // Fire kill event for attacker
        if (data.isMurder) {
            val attacker = match.participants[data.attacker?.id]!!

            var participantContext = ParticipantContext(attacker, match)
            participantListeners.forEach { participantContext = it.onKill(participantContext, data, isFirstBlood) }
            match.saveParticipants(participantContext.profile)

            var playerContext = participantContext.getPlayerContext()
            playerListeners.forEach { playerContext = it.onKill(playerContext, data, isFirstBlood) }
            attacker.setPlayer(playerContext.profile)
        }

        // Fire death event for victim
        val victim = match.participants[data.victim.id]!!

        var participantContext = ParticipantContext(victim, match)
        participantListeners.forEach { participantContext = it.onDeath(participantContext, data, isFirstBlood) }
        match.saveParticipants(participantContext.profile)

        var playerContext = participantContext.getPlayerContext()
        playerListeners.forEach { playerContext = it.onDeath(playerContext, data, isFirstBlood) }
        victim.setPlayer(playerContext.profile)

        // Save Death DB object (move to future Match Listener ideally)
        Database.deaths.insertOne(
            Death(
                _id = UUID.randomUUID().toString(),
                victim = data.victim,
                attacker = data.attacker,
                weapon = data.weapon,
                entity = data.entity,
                distance = data.distance,
                key = data.key,
                cause = data.cause,
                matchId = match._id,
                serverId = server.id,
                createdAt = Date().time
            )
        )

        // Save
        MatchCache.set(match._id, match)
    }

    private suspend fun onPlayerChat(data: PlayerChatData) {
        val match = server.match ?: throw InvalidMatchStateException()
        val participant = match.participants[data.player.id]

        if (participant != null) {
            var participantContext = ParticipantContext(participant, match)
            participantListeners.forEach { participantContext = it.onChat(participantContext, data) }
            match.saveParticipants(participantContext.profile)
        }

        val player: Player = PlayerCache.get(data.player.name) ?: return
        var playerContext = PlayerContext(player, match)
        playerListeners.forEach { playerContext = it.onChat(playerContext, data) }
        PlayerCache.set(player.name, playerContext.profile)

        MatchCache.set(match._id, match)
    }

    private suspend fun onKillstreak(data: KillstreakData) {
        val match =
            server.match ?: throw InvalidMatchStateException()
        if (match.state != MatchState.IN_PROGRESS) throw InvalidMatchStateException()
        val participant = match.participants[data.player.id]!!

        var participantContext = ParticipantContext(participant, match)
        var playerContext = participantContext.getPlayerContext()
        if (data.ended) {
            participantListeners.forEach { participantContext = it.onKillstreakEnd(participantContext, data.amount) }
            playerListeners.forEach { playerContext = it.onKillstreakEnd(playerContext, data.amount) }
        } else {
            participantListeners.forEach { participantContext = it.onKillstreak(participantContext, data.amount) }
            playerListeners.forEach { playerContext = it.onKillstreak(playerContext, data.amount) }
        }
        match.saveParticipants(participantContext.profile)
        participant.setPlayer(playerContext.profile)

        MatchCache.set(match._id, match)
    }

    private suspend fun onPartyJoin(data: PartyJoinData) {
        val match = server.match ?: throw InvalidMatchStateException()
        if (match.state != MatchState.IN_PROGRESS) throw InvalidMatchStateException()
        val participant = match.participants[data.player.id] ?: Participant(
            SimpleParticipant(
                data.player.name,
                data.player.id,
                data.partyName
            )
        )

        var participantContext = ParticipantContext(participant, match)
        participantListeners.forEach { participantContext = it.onPartyJoin(participantContext, data.partyName) }
        match.saveParticipants(participantContext.profile)

        var playerContext = participantContext.getPlayerContext()
        playerListeners.forEach { playerContext = it.onPartyJoin(playerContext, data.partyName) }
        participant.setPlayer(playerContext.profile)

        MatchCache.set(match._id, match)
    }

    private suspend fun onPartyLeave(data: PartyLeaveData) {
        val match = server.match ?: throw InvalidMatchStateException()
        val participant = match.participants[data.player.id]!!

        var participantContext = ParticipantContext(participant, match)
        participantListeners.forEach { participantContext = it.onPartyLeave(participantContext) }
        match.saveParticipants(participantContext.profile)

        var playerContext = participantContext.getPlayerContext()
        playerListeners.forEach { playerContext = it.onPartyLeave(playerContext) }
        participant.setPlayer(playerContext.profile)

        match.saveParticipants(participantContext.profile)
        MatchCache.set(match._id, match)
    }

    private suspend fun onDestroyableDamage(data: DestroyableDamageData) {
        val match =
            server.match ?: throw InvalidMatchStateException()

        val participant = match.participants[data.playerId]!!
        val destroyable = match.level.goals?.destroyables?.find { it.id == data.destroyableId } ?: return

        var participantContext = ParticipantContext(participant, match)
        participantListeners.forEach {
            participantContext = it.onDestroyableDamage(participantContext, destroyable, data.damage)
        }
        match.saveParticipants(participantContext.profile)

        var playerContext = participantContext.getPlayerContext()
        playerListeners.forEach { playerContext = it.onDestroyableDamage(playerContext, destroyable, data.damage) }
        participant.setPlayer(playerContext.profile)

        match.saveParticipants(participantContext.profile)
        MatchCache.set(match._id, match)
    }

    private suspend fun onDestroyableDestroy(data: DestroyableDestroyData) {
        val match =
            server.match ?: throw InvalidMatchStateException()

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
        val match = server.match ?: throw InvalidMatchStateException()

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
        val match = server.match ?: throw InvalidMatchStateException()
        val participant = match.participants[data.playerId]!!

        var participantContext = ParticipantContext(participant, match)
        participantListeners.forEach { participantContext = it.onFlagPlace(participantContext, data.heldTime) }

        var playerContext = participantContext.getPlayerContext()
        playerListeners.forEach { playerContext = it.onFlagPlace(playerContext, data.heldTime) }
        participant.setPlayer(playerContext.profile)

        match.saveParticipants(participantContext.profile)
        MatchCache.set(match._id, match)
    }

    private suspend fun onFlagPickup(data: FlagEventData) {
        val match = server.match ?: throw InvalidMatchStateException()
        if (match.state != MatchState.IN_PROGRESS) throw InvalidMatchStateException()
        val participant = match.participants[data.playerId]!!

        var participantContext = ParticipantContext(participant, match)
        participantListeners.forEach { participantContext = it.onFlagPickup(participantContext) }
        match.saveParticipants(participantContext.profile)

        var playerContext = participantContext.getPlayerContext()
        playerListeners.forEach { playerContext = it.onFlagPickup(playerContext) }
        participant.setPlayer(playerContext.profile)

        MatchCache.set(match._id, match)
    }

    private suspend fun onFlagDrop(data: FlagDropData) {
        val match = server.match ?: throw InvalidMatchStateException()
        if (match.state != MatchState.IN_PROGRESS) throw InvalidMatchStateException()
        val participant = match.participants[data.playerId]!!

        var participantContext = ParticipantContext(participant, match)
        participantListeners.forEach { participantContext = it.onFlagDrop(participantContext, data.heldTime) }
        match.saveParticipants(participantContext.profile)

        var playerContext = participantContext.getPlayerContext()
        playerListeners.forEach { playerContext = it.onFlagDrop(playerContext, data.heldTime) }
        participant.setPlayer(playerContext.profile)

        MatchCache.set(match._id, match)
    }

    private suspend fun onFlagDefend(data: FlagEventData) {
        val match = server.match ?: throw InvalidMatchStateException()
        if (match.state != MatchState.IN_PROGRESS) throw InvalidMatchStateException()
        val participant = match.participants[data.playerId]!!

        var participantContext = ParticipantContext(participant, match)
        participantListeners.forEach { participantContext = it.onFlagDefend(participantContext) }
        match.saveParticipants(participantContext.profile)

        var playerContext = participantContext.getPlayerContext()
        playerListeners.forEach { playerContext = it.onFlagDefend(playerContext) }
        participant.setPlayer(playerContext.profile)

        MatchCache.set(match._id, match)
    }

    private suspend fun onWoolPlace(data: WoolDropData) {
        val match = server.match ?: throw InvalidMatchStateException()
        val participant = match.participants[data.playerId]!!

        var participantContext = ParticipantContext(participant, match)
        participantListeners.forEach { participantContext = it.onWoolPlace(participantContext, data.heldTime) }
        match.saveParticipants(participantContext.profile)

        var playerContext = participantContext.getPlayerContext()
        playerListeners.forEach { playerContext = it.onWoolPlace(playerContext, data.heldTime) }
        participant.setPlayer(playerContext.profile)

        MatchCache.set(match._id, match)
    }

    private suspend fun onWoolPickup(data: WoolEventData) {
        val match = server.match ?: throw InvalidMatchStateException()
        if (match.state != MatchState.IN_PROGRESS) throw InvalidMatchStateException()
        val participant = match.participants[data.playerId]!!

        var participantContext = ParticipantContext(participant, match)
        participantListeners.forEach { participantContext = it.onWoolPickup(participantContext) }
        match.saveParticipants(participantContext.profile)

        var playerContext = participantContext.getPlayerContext()
        playerListeners.forEach { playerContext = it.onWoolPickup(playerContext) }
        participant.setPlayer(playerContext.profile)

        MatchCache.set(match._id, match)
    }

    private suspend fun onWoolDrop(data: WoolDropData) {
        val match = server.match ?: throw InvalidMatchStateException()
        if (match.state != MatchState.IN_PROGRESS) throw InvalidMatchStateException()
        val participant = match.participants[data.playerId]!!

        var participantContext = ParticipantContext(participant, match)
        participantListeners.forEach { participantContext = it.onWoolDrop(participantContext, data.heldTime) }
        match.saveParticipants(participantContext.profile)

        var playerContext = participantContext.getPlayerContext()
        playerListeners.forEach { playerContext = it.onWoolDrop(playerContext, data.heldTime) }
        participant.setPlayer(playerContext.profile)

        MatchCache.set(match._id, match)
    }

    private suspend fun onWoolDefend(data: WoolEventData) {
        val match = server.match ?: throw InvalidMatchStateException()
        if (match.state != MatchState.IN_PROGRESS) throw InvalidMatchStateException()
        val participant = match.participants[data.playerId]!!

        var participantContext = ParticipantContext(participant, match)
        participantListeners.forEach { participantContext = it.onWoolDefend(participantContext) }
        match.saveParticipants(participantContext.profile)

        var playerContext = participantContext.getPlayerContext()
        playerListeners.forEach { playerContext = it.onWoolDefend(playerContext) }
        participant.setPlayer(playerContext.profile)

        MatchCache.set(match._id, match)
    }

    private suspend fun onControlPointCapture(data: ControlPointCaptureData) {
        val match = server.match ?: throw InvalidMatchStateException()
        if (match.state != MatchState.IN_PROGRESS) throw InvalidMatchStateException()

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

    private suspend fun onAchievementComplete(data: PlayerAchievementData) {
        val player: Player = PlayerCache.get(data.player.name) ?: return
        player.stats.achievements[data.achievement] = AchievementStatistic(completionTime=data.completionTime)
        PlayerCache.set(player.name, player, true)
    }
}

class InvalidMatchStateException : IllegalStateException("Encountered invalid match state or missing match")