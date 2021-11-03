package network.warzone.api.database.models

import kotlinx.serialization.Serializable
import network.warzone.api.database.PlayerCache
import kotlin.collections.Map

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

    constructor(simple: SimpleParticipant) : this(
        simple.name,
        simple.id,
        simple.partyName,
        simple.partyName,
        System.currentTimeMillis(),
        System.currentTimeMillis(),
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
    var objectives: PlayerObjectiveStatistics = PlayerObjectiveStatistics(),
    var bowShotsTaken: Int = 0,
    var bowShotsHit: Int = 0,
    var blocksPlaced: HashMap<String, Int> = hashMapOf(),
    var blocksBroken: HashMap<String, Int> = hashMapOf(),
    var damageTaken: Double = 0.0,
    var damageGiven: Double = 0.0,
    var damageGivenBow: Double = 0.0,
    var messages: PlayerMessages = PlayerMessages(),
    var weapons: MutableMap<String, WeaponDamageData> = mutableMapOf(),
    var killstreaks: Map<Int, Int> = emptyMap(), // todo
    var duels: MutableMap<String, Duel> = mutableMapOf()
)

@Serializable
data class Duel(var kills: Int = 0, var deaths: Int = 0)