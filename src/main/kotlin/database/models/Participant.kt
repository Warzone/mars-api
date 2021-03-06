package network.warzone.api.database.models

import kotlinx.serialization.Serializable
import network.warzone.api.database.PlayerCache
import java.util.*

@Serializable
data class SimpleParticipant(val name: String, val id: String, var partyName: String?)

@Serializable
data class Participant(
    val name: String,
    val id: String,
    var partyName: String? = null,
    var lastPartyName: String? = null,
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

    suspend fun getPlayer(): Player {
        return PlayerCache.get(nameLower)!!
    }

    suspend fun setPlayer(player: Player) {
        return PlayerCache.set(nameLower, player)
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

    /**
     * If name is Notch & ID is 069a79f4-44e9-4726-a5be-fca90e38aaf5
     * Encoded string is 069a79f4-44e9-4726-a5be-fca90e38aaf5/Notch
     */
    val idName: String
        get() = "${id}/${name}"
}

@Serializable
data class ParticipantStats(
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
    val killstreaks: MutableMap<Int, Int> = mutableMapOf(),
    val killstreaksEnded: MutableMap<Int, Int> = mutableMapOf(),
    var duels: MutableMap<String, Duel> = mutableMapOf()
)

@Serializable
data class Duel(var kills: Int = 0, var deaths: Int = 0)