package network.warzone.api.database.models

import kotlinx.serialization.Serializable
import network.warzone.api.database.Database
import org.litote.kmongo.SetTo
import org.litote.kmongo.and
import org.litote.kmongo.eq
import org.litote.kmongo.not
import kotlin.collections.Map

@Serializable
data class Player(
    val _id: String,
    var name: String,
    var nameLower: String,
    var firstJoinedAt: Long,
    var lastJoinedAt: Long,
    var ips: List<String>,
    var rankIds: List<String>,
    var tagIds: List<String>,
    var activeTagId: String?,
    val stats: PlayerStats
) {
    suspend fun getActiveSession(): Session? {
        return Database.sessions.findOne(Session::endedAt eq null, Session::playerId eq _id)
    }

    companion object {
        suspend fun ensureNameUniqueness(name: String, keepId: String) {
            val tempName = ">>awarzoneplayer${(0..1000).random()}"
            Database.players.updateMany(
                and(Player::nameLower eq name.lowercase(), not(Player::_id eq keepId)),
                SetTo(Player::name, tempName),
                SetTo(Player::nameLower, tempName)
            )
        }
    }
}

@Serializable
data class SimplePlayer(val name: String, val id: String)

@Serializable
data class PlayerStats(
    var xp: Int = 0,
    var serverPlaytime: Long = 0,
    var gamePlaytime: Long = 0,
    var kills: Int = 0,
    var deaths: Int = 0,
    var voidKills: Int = 0,
    var voidDeaths: Int = 0,
    var firstBloods: Int = 0,
    var firstBloodsSuffered: Int = 0,
    var objectives: PlayerObjectiveStatistics = PlayerObjectiveStatistics(),
    var bowShotsTaken: Int = 0,
    var bowShotsHit: Int = 0,
    var blocksPlaced: HashMap<String, Int> = hashMapOf(),
    var blocksBroken: HashMap<String, Int> = hashMapOf(),
    var damageTaken: Int = 0,
    var damageGiven: Int = 0,
    var messages: Int = 0,
    var wins: Int = 0,
    var losses: Int = 0,
    var ties: Int = 0,
    var matches: Int = 0,
    var matchesPresentStart: Int = 0,
    var matchesPresentFull: Int = 0,
    var mvps: Int = 0,
    var records: PlayerRecords = PlayerRecords(),
    var weapons: MutableMap<String, WeaponDamageData> = mutableMapOf(),
    var killstreaks: Map<Int, Int> = emptyMap(),
)

@Serializable
data class PlayerRecords(
    var highestKillstreak: Int = 0,
    var longestSession: Int = 0,
    var longestBowShot: Int = 0,
    var fastestWoolCapture: Int = 0,
    var fastestFirstBlood: Int = 0,
    var fastestFlagCapture: Int = 0,
    var killsPerMatch: Int = 0,
    var deathsInMatch: Int = 0,
    var highestScore: Int = 0
)

@Serializable
data class WeaponDamageData(var kills: Int)

@Serializable
data class PlayerObjectiveStatistics(
    var coreLeaks: Int = 0,
    var destroyableDestroys: Int = 0,
    var destroyableBlockDestroys: Int = 0,
    var flagCaptures: Int = 0,
    var flagPickups: Int = 0,
    var flagDrops: Int = 0,
    var flagDefends: Int = 0,
    var totalFlagHoldTime: Long = 0,
    var woolCaptures: Int = 0,
    var woolDrops: Int = 0,
    var woolDefends: Int = 0,
    var woolPickups: Int = 0,
    var controlPointCaptures: Int = 0
)

@Serializable
data class PlayerMessages(var staff: Int = 0, var global: Int = 0, var team: Int = 0)