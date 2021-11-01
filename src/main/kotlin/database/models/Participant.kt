package network.warzone.api.database.models

import kotlinx.serialization.Serializable
import kotlin.collections.Map

@Serializable
data class SimpleParticipant(val name: String, val id: String, var partyName: String?)

@Serializable
data class Participant(
    val name: String,
    val id: String,
    var partyName: String?,
    var lastPartyName: String?,
    val stats: ParticipantStats
) {
    val simplePlayer = SimplePlayer(name, id)

    constructor(simple: SimpleParticipant) : this(
        simple.name,
        simple.id,
        simple.partyName,
        simple.partyName,
        ParticipantStats()
    )
}

@Serializable
data class ParticipantStats(
    var xp: Int = 0,
    var serverPlaytime: Long = 0,
    var gamePlaytime: Long = 0,
    var kills: Int = 0,
    var deaths: Int = 0,
    var voidKills: Int = 0,
    var voidDeaths: Int = 0,
    var objectives: PlayerObjectiveStatistics = PlayerObjectiveStatistics(),
    var bowShotsTaken: Int = 0,
    var bowShotsHit: Int = 0,
    var blocksPlaced: Int = 0, // change data types
    var blocksBroken: Int = 0, // ^^^
    var damageTaken: Double = 0.0,
    var damageGiven: Double = 0.0,
    var messages: Int = 0, // change data type
    var weapons: MutableMap<String, WeaponDamageData> = mutableMapOf(),
    var killstreaks: Map<Int, Int> = emptyMap(), // send at end
    var duels: MutableMap<String, Duel> = mutableMapOf()
)