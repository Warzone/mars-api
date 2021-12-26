package network.warzone.api.socket.listeners.stats

import kotlinx.serialization.Serializable
import network.warzone.api.database.MatchCache
import network.warzone.api.database.PlayerCache
import network.warzone.api.database.models.Match
import network.warzone.api.database.models.Participant
import network.warzone.api.database.models.Player
import network.warzone.api.database.models.PlayerMatchResult
import network.warzone.api.socket.event.EventPriority
import network.warzone.api.socket.event.EventType
import network.warzone.api.socket.event.FireAt
import network.warzone.api.socket.event.Listener
import network.warzone.api.socket.listeners.death.PlayerDeathEvent
import network.warzone.api.socket.listeners.match.MatchEndEvent
import network.warzone.api.socket.listeners.objective.*
import network.warzone.api.socket.listeners.server.LiveGameServer
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

const val XP_PER_LEVEL = 5000
const val XP_BEGINNER_ASSIST = 10

const val XP_WIN = 200
const val XP_LOSS = 100
const val XP_DRAW = 150
const val XP_KILL = 40
const val XP_DEATH = 1
const val XP_FIRST_BLOOD = 7
const val XP_WOOL_OBJECTIVE = 60
const val XP_FLAG_OBJECTIVE = 90
const val XP_POINT_CAPTURE_MAX = 100
const val XP_DESTROYABLE_WHOLE = 200
const val XP_KILLSTREAK_COEFFICIENT = 10
const val XP_KILLSTREAK_END_COEFFICIENT = 2

fun gain(gain: Int, level: Int): Int {
    val rawMultiplier = XP_BEGINNER_ASSIST - level
    val multiplier = if (rawMultiplier >= 1) rawMultiplier else 1
    return gain * multiplier
}

suspend fun sendXPNotification(server: LiveGameServer, gain: Int, reason: String, vararg playerIds: String) {
    println("Send xp notification $reason")
    server.call(EventType.MESSAGE, MessageData("&d+$gain XP ($reason)", "ORB_PICKUP", playerIds.toList()))
}

suspend fun addXP(match: Match, participant: Participant, player: Player, xp: Int) {
    val currentLevel = player.stats.level

    participant.stats.xp += xp
    match.saveParticipants(participant)
    MatchCache.set(match._id, match)

    player.stats.xp += xp
    if (player.stats.level > currentLevel) match.server.call(
        EventType.PLAYER_LEVEL_UP,
        PlayerLevelUpData(player._id, player.stats.level, player.stats.xp)
    )

    participant.setPlayer(player)
}

suspend fun awardWoolObjective(match: Match, participant: Participant, reason: String) {
    val player = participant.getPlayer()!!
    val xp = gain(XP_WOOL_OBJECTIVE, player.stats.level)
    addXP(match, participant, player, xp)
    sendXPNotification(match.server, xp, reason, player._id)
}

suspend fun awardFlagObjective(match: Match, participant: Participant, reason: String) {
    val player = participant.getPlayer()!!
    val xp = gain(XP_FLAG_OBJECTIVE, player.stats.level)
    addXP(match, participant, player, xp)
    sendXPNotification(match.server, xp, reason, player._id)
}


class PlayerXPListener : Listener() {
    override val handlers = mapOf(
        ::onKill to PlayerDeathEvent::class,
        ::onDeath to PlayerDeathEvent::class,
        ::onWoolCapture to WoolPlaceEvent::class,
        ::onWoolPickup to WoolPickupEvent::class,
        ::onWoolDefend to WoolDefendEvent::class,
        ::onFlagCapture to FlagPlaceEvent::class,
        ::onFlagPickup to FlagPickupEvent::class,
        ::onFlagDefend to FlagDefendEvent::class,
        ::onControlPointCapture to ControlPointCaptureEvent::class,
        ::onDestroyableDestroy to DestroyableDestroyEvent::class,
        ::onCoreLeak to CoreLeakEvent::class,
        ::onKillstreak to KillstreakEvent::class,
        ::onKillstreakEnd to KillstreakEvent::class,
        ::onMatchWin to MatchEndEvent::class,
        ::onMatchLose to MatchEndEvent::class,
        ::onMatchDraw to MatchEndEvent::class
    )

    @FireAt(EventPriority.LATEST)
    suspend fun onKill(event: PlayerDeathEvent) {
        if (event.attacker == null || event.victim.id == event.attacker.id) return

        val firstBlood = event.match.firstBlood!!
        val isFirstBlood =
            firstBlood.attacker.id == event.attacker.id && firstBlood.victim.id == event.victim.id && (Date().time - firstBlood.date) < 5000

        val player = event.attacker.getPlayer()!!

        val killXP = gain(XP_KILL, player.stats.level)
        val firstBloodXP = if (isFirstBlood) gain(XP_FIRST_BLOOD, player.stats.level) else 0

        sendXPNotification(event.server, killXP, "Kill", player._id)
        if (isFirstBlood) sendXPNotification(event.server, firstBloodXP, "First blood", player._id)

        addXP(event.match, event.attacker, player, killXP + firstBloodXP)
    }

    @FireAt(EventPriority.LATEST)
    suspend fun onDeath(event: PlayerDeathEvent) {
        val player = event.victim.getPlayer()!!
        val xp = gain(XP_DEATH, player.stats.level)
        addXP(event.match, event.victim, player, xp)
    }

    @FireAt(EventPriority.LATEST)
    suspend fun onWoolCapture(event: WoolPlaceEvent) {
        awardWoolObjective(event.match, event.participant, "Placed wool")
    }

    @FireAt(EventPriority.LATEST)
    suspend fun onWoolPickup(event: WoolPickupEvent) {
        awardWoolObjective(event.match, event.participant, "Picked up wool")
    }

    @FireAt(EventPriority.LATEST)
    suspend fun onWoolDefend(event: WoolDefendEvent) {
        awardWoolObjective(event.match, event.participant, "Defended wool")
    }

    @FireAt(EventPriority.LATEST)
    suspend fun onFlagCapture(event: FlagPlaceEvent) {
        awardFlagObjective(event.match, event.participant, "Placed flag")
    }

    @FireAt(EventPriority.LATEST)
    suspend fun onFlagPickup(event: FlagPickupEvent) {
        awardFlagObjective(event.match, event.participant, "Picked up flag")
    }

    @FireAt(EventPriority.LATEST)
    suspend fun onFlagDefend(event: FlagDefendEvent) {
        awardFlagObjective(event.match, event.participant, "Defended flag")
    }

    @FireAt(EventPriority.LATEST)
    suspend fun onKillstreak(event: KillstreakEvent) {
        if (event.data.ended) return
        val (amount) = event.data
        val player = event.participant.getPlayer()!!

        val xp = gain(amount * XP_KILLSTREAK_COEFFICIENT, player.stats.level)

        sendXPNotification(event.server, xp, "Killstreak x${amount}", player._id)
        addXP(event.match, event.participant, player, xp)
    }

    @FireAt(EventPriority.LATEST)
    suspend fun onKillstreakEnd(event: KillstreakEvent) {
        if (!event.data.ended) return
        val (amount) = event.data
        val player = event.participant.getPlayer()!!

        val xp = gain(amount * XP_KILLSTREAK_END_COEFFICIENT, player.stats.level)

        sendXPNotification(event.server, xp, "Ended x${amount} killstreak", player._id)
        addXP(event.match, event.participant, player, xp)
    }

    @FireAt(EventPriority.LATEST)
    suspend fun onMatchWin(event: MatchEndEvent) {
        if (event.isTie()) return

        // min ( 10% of match length | 1 minute )
        val minimumPlaytime = min(0.10 * event.match.length, 60000.0)

        val winners =
            event.match.participants.filterValues { it.resultInMatch(event) == PlayerMatchResult.WIN && it.stats.gamePlaytime > minimumPlaytime }
        winners.forEach { (id, participant) ->
            val player = participant.getPlayer()!!
            addXP(event.match, participant, player, XP_WIN)
            PlayerCache.persist<Player>(id)
        }
        sendXPNotification(event.server, XP_WIN, "Victory", *winners.map { it.key }.toTypedArray())
    }

    @FireAt(EventPriority.LATEST)
    suspend fun onMatchLose(event: MatchEndEvent) {
        if (event.isTie()) return

        // min ( 10% of match length | 1 minute )
        val minimumPlaytime = min(0.10 * event.match.length, 60000.0)

        val losers =
            event.match.participants.filterValues { it.resultInMatch(event) == PlayerMatchResult.LOSE && it.stats.gamePlaytime > minimumPlaytime }
        losers.forEach { (id, participant) ->
            val player = participant.getPlayer()!!
            addXP(event.match, participant, player, XP_LOSS)
            PlayerCache.persist<Player>(id)
        }
        sendXPNotification(event.server, XP_LOSS, "Defeat", *losers.map { it.key }.toTypedArray())
    }

    @FireAt(EventPriority.LATEST)
    suspend fun onMatchDraw(event: MatchEndEvent) {
        if (!event.isTie()) return

        // min ( 10% of match length | 1 minute )
        val minimumPlaytime = min(0.10 * event.match.length, 60000.0)

        val players = event.match.participants.filterValues { it.stats.gamePlaytime > minimumPlaytime }
        players.forEach { (id, participant) ->
            val player = participant.getPlayer()!!
            addXP(event.match, participant, player, XP_DRAW)
            PlayerCache.persist<Player>(id)
        }
        sendXPNotification(event.server, XP_DRAW, "Tie", *players.map { it.key }.toTypedArray())
    }

    @FireAt(EventPriority.LATEST)
    suspend fun onControlPointCapture(event: ControlPointCaptureEvent) {
        val contributors = event.data.playerIds.count() - 1
        event.data.playerIds.mapNotNull { event.match.participants[it] }.forEach {
            val player = it.getPlayer()!!
            val xp = gain(max(XP_POINT_CAPTURE_MAX - (contributors * 10), 20), player.stats.level)
            sendXPNotification(event.server, xp, "Captured point", it.id)
            addXP(event.match, it, player, xp)
        }
    }

    @FireAt(EventPriority.LATEST)
    suspend fun onDestroyableDestroy(event: DestroyableDestroyEvent) {
        event.data.contributions.forEach {
            val participant = event.match.participants[it.playerId] ?: return
            val player = participant.getPlayer()!!
            val xp = gain(round(it.percentage * XP_DESTROYABLE_WHOLE).toInt(), player.stats.level)
            sendXPNotification(event.server, xp, "Destroyed objective", it.playerId)
            addXP(event.match, participant, player, xp)
        }
    }

    @FireAt(EventPriority.LATEST)
    suspend fun onCoreLeak(event: CoreLeakEvent) {
        event.data.contributions.forEach {
            val participant = event.match.participants[it.playerId] ?: return
            val player = participant.getPlayer()!!
            val xp = gain(round(it.percentage * XP_DESTROYABLE_WHOLE).toInt(), player.stats.level)
            sendXPNotification(event.server, xp, "Destroyed core", it.playerId)
            addXP(event.match, participant, player, xp)
        }
    }
}

@Serializable
data class MessageData(val message: String, val sound: String?, val players: List<String>)

@Serializable
data class PlayerLevelUpData(val playerId: String, val newLevel: Int, val xp: Int)