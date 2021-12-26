package network.warzone.api.database.models

import kotlinx.serialization.Serializable
import network.warzone.api.database.PlayerCache
import network.warzone.api.socket.listeners.match.MatchEndEvent
import java.util.*

@Serializable
data class SimpleParticipant(val name: String, val id: String, var partyName: String?)

@Serializable
data class Participant(
    val name: String,
    val id: String,
    var partyName: String?,
    var lastPartyName: String?,
    var firstJoinedMatchAt: Long,
    var joinedPartyAt: Long? = null,
    var lastLeftPartyAt: Long? = null,
    val stats: ParticipantStats
) {
    val nameLower: String
        get() {
            return name.lowercase()
        }

    val simplePlayer = SimplePlayer(name, id)

    suspend fun getPlayer(): Player? {
        return PlayerCache.get(nameLower)
    }

    suspend fun setPlayer(player: Player) {
        return PlayerCache.set(nameLower, player)
    }

    fun resultInMatch(end: MatchEndEvent): PlayerMatchResult {
        val isPlaying = this.partyName != null
        if (!isPlaying) return PlayerMatchResult.INDETERMINATE

        return when {
            end.isTie() -> PlayerMatchResult.TIE
            !end.isTie() && end.data.winningParties.contains(this.partyName) -> PlayerMatchResult.WIN
            !end.data.winningParties.contains(this.partyName) -> PlayerMatchResult.LOSE
            else -> PlayerMatchResult.INDETERMINATE
        }
    }

    constructor(simple: SimpleParticipant) : this(
        simple.name,
        simple.id,
        simple.partyName,
        simple.partyName,
        Date().time,
        Date().time,
        null,
        ParticipantStats()
    )
}

@Serializable
data class ParticipantStats(
    var xp: Int = 0, // todo
    var gamePlaytime: Long = 0,
    var timeAway: Long = 0,
    var kills: Int = 0,
    var deaths: Int = 0,
    var voidKills: Int = 0,
    var voidDeaths: Int = 0,
    val objectives: PlayerObjectiveStatistics = PlayerObjectiveStatistics(),
    var bowShotsTaken: Int = 0,
    var bowShotsHit: Int = 0,
    val blocksPlaced: HashMap<String, Int> = hashMapOf(),
    val blocksBroken: HashMap<String, Int> = hashMapOf(),
    var damageTaken: Double = 0.0,
    var damageGiven: Double = 0.0,
    var damageGivenBow: Double = 0.0,
    val messages: PlayerMessages = PlayerMessages(),
    val weaponKills: MutableMap<String, Int> = mutableMapOf(),
    val weaponDeaths: MutableMap<String, Int> = mutableMapOf(),
    val killstreaks: MutableMap<Int, Int> = mutableMapOf(5 to 0, 10 to 0, 25 to 0, 50 to 0, 100 to 0),
    var duels: MutableMap<String, Duel> = mutableMapOf()
)

@Serializable
data class Duel(var kills: Int = 0, var deaths: Int = 0)

@Serializable
enum class PlayerMatchResult {
    WIN,
    LOSE,
    TIE,
    INDETERMINATE
}