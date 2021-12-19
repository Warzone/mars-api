package network.warzone.api.database.models

import kotlinx.serialization.Serializable
import network.warzone.api.database.Database
import org.litote.kmongo.*
import java.util.*

@Serializable
data class Player(
    val _id: String,
    var name: String,
    var nameLower: String,
    var firstJoinedAt: Long,
    var lastJoinedAt: Long,
    var ips: List<String>,
    var notes: List<StaffNote> = emptyList(),
    var rankIds: List<String>,
    var tagIds: List<String>,
    var activeTagId: String?,
    val stats: PlayerStats
) {
    suspend fun getActiveSession(): Session? {
        return Database.sessions.findOne(Session::endedAt eq null, Session::playerId eq _id)
    }

    suspend fun getPunishments(): List<Punishment> {
        return Database.punishments.find(Punishment::target / SimplePlayer::id eq this._id).toList().sortedBy { it.issuedAt }
    }

    suspend fun getActivePunishments(): List<Punishment> {
        val now = Date().time
        return getPunishments().filter { now < it.expiresAt || it.action.length == -1L }.sortedBy { it.issuedAt }
    }

    // note: only first degree alts atm
    suspend fun getAlts(): List<Player> {
        return Database.players.find(Player::ips `in` this.ips, Player::_id ne this._id).toList()
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

    val simple: SimplePlayer
    get() {
        return SimplePlayer(name = this.name, id = this._id)
    }
}

@Serializable
data class SimplePlayer(val name: String, val id: String)

@Serializable
data class PlayerStats(
    var xp: Int = 0, // todo
    var serverPlaytime: Long = 0,
    var gamePlaytime: Long = 0,
    var kills: Int = 0,
    var deaths: Int = 0,
    var voidKills: Int = 0,
    var voidDeaths: Int = 0,
    var firstBloods: Int = 0,
    var firstBloodsSuffered: Int = 0,
    val objectives: PlayerObjectiveStatistics = PlayerObjectiveStatistics(),
    var bowShotsTaken: Int = 0,
    var bowShotsHit: Int = 0,
    val blocksPlaced: HashMap<String, Int> = hashMapOf(),
    val blocksBroken: HashMap<String, Int> = hashMapOf(),
    var damageTaken: Double = 0.0,
    var damageGiven: Double = 0.0,
    var damageGivenBow: Double = 0.0,
    val messages: PlayerMessages = PlayerMessages(),
    var wins: Int = 0,
    var losses: Int = 0,
    var ties: Int = 0,
    var matches: Int = 0,
    var matchesPresentStart: Int = 0,
    var matchesPresentFull: Int = 0,
    var matchesPresentEnd: Int = 0,
    var mvps: Int = 0, // todo
    val records: PlayerRecords = PlayerRecords(), // todo
    val weaponKills: MutableMap<String, Int> = mutableMapOf(),
    val killstreaks: MutableMap<Int, Int> = mutableMapOf(5 to 0, 10 to 0, 25 to 0, 50 to 0, 100 to 0),
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