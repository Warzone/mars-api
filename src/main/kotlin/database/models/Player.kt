package network.warzone.api.database.models

import kotlinx.serialization.Serializable
import network.warzone.api.database.Database
import network.warzone.api.socket.leaderboard.ScoreType
import network.warzone.api.socket.player.XP_PER_LEVEL
import org.litote.kmongo.*
import kotlin.math.floor

@Serializable
data class Player(
    val _id: String,
    var name: String,
    var nameLower: String,
    var lastSessionId: String,
    var firstJoinedAt: Long,
    var lastJoinedAt: Long,
    var ips: List<String>,
    var notes: List<StaffNote>,
    var rankIds: List<String>,
    var tagIds: List<String>,
    var activeTagId: String?,
    val stats: PlayerStats,
    val gamemodeStats: HashMap<LevelGamemode, GamemodeStats>
) {
    suspend fun getActiveSession(): Session? {
        return Database.sessions.findOne(Session::endedAt eq null, Session::player / SimplePlayer::id eq _id)
    }

    suspend fun getPunishments(): List<Punishment> {
        return Database.punishments.find(Punishment::target / SimplePlayer::id eq this._id).toList()
            .sortedBy { it.issuedAt }
    }

    suspend fun getActivePunishments(): List<Punishment> {
        return getPunishments().filter { it.isActive }.sortedBy { it.issuedAt }
    }

    // note: only first degree alts atm
    suspend fun getAlts(): List<Player> {
        return Database.players.find(Player::ips `in` this.ips, Player::_id ne this._id).toList()
    }

    companion object {
        suspend fun ensureNameUniqueness(name: String, keepId: String) {
            val tempName = ">WZPlayer${(0..1000).random()}"
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

    /**
     * If name is Notch & ID is 069a79f4-44e9-4726-a5be-fca90e38aaf5
     * Encoded string is 069a79f4-44e9-4726-a5be-fca90e38aaf5/Notch
     */
    val idName: String
        get() = "${_id}/${name}"
}

@Serializable
data class SimplePlayer(val name: String, val id: String)

typealias GamemodeStats = PlayerStats

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
    val records: PlayerRecords = PlayerRecords(),
    val weaponKills: MutableMap<String, Int> = mutableMapOf(),
    val weaponDeaths: MutableMap<String, Int> = mutableMapOf(),
    val killstreaks: MutableMap<Int, Int> = mutableMapOf(),
    val killstreaksEnded: MutableMap<Int, Int> = mutableMapOf(),
) {
    val level: Int
        get() = floor(((xp + XP_PER_LEVEL) / XP_PER_LEVEL).toDouble()).toInt()

    fun getScore(type: ScoreType): Int {
        return when (type) {
            ScoreType.KILLS -> kills
            ScoreType.DEATHS -> deaths
            ScoreType.FIRST_BLOODS -> firstBloods
            ScoreType.WINS -> wins
            ScoreType.LOSSES -> losses
            ScoreType.TIES -> ties
            ScoreType.XP -> xp
            ScoreType.MESSAGES_SENT -> messages.total
            ScoreType.MATCHES_PLAYED -> matches
            ScoreType.SERVER_PLAYTIME -> serverPlaytime.toInt() // 2038
            ScoreType.GAME_PLAYTIME -> gamePlaytime.toInt()
            ScoreType.CORE_LEAKS -> objectives.coreLeaks
            ScoreType.CORE_BLOCK_DESTROYS -> objectives.coreBlockDestroys
            ScoreType.DESTROYABLE_DESTROYS -> objectives.destroyableDestroys
            ScoreType.DESTROYABLE_BLOCK_DESTROYS -> objectives.destroyableBlockDestroys
            ScoreType.FLAG_CAPTURES -> objectives.flagCaptures
            ScoreType.FLAG_DROPS -> objectives.flagDrops
            ScoreType.FLAG_PICKUPS -> objectives.flagPickups
            ScoreType.FLAG_DEFENDS -> objectives.flagDefends
            ScoreType.FLAG_HOLD_TIME -> objectives.totalFlagHoldTime.toInt()
            ScoreType.WOOL_CAPTURES -> objectives.woolCaptures
            ScoreType.WOOL_DROPS -> objectives.woolDrops
            ScoreType.WOOL_PICKUPS -> objectives.woolPickups
            ScoreType.WOOL_DEFENDS -> objectives.woolDefends
            ScoreType.CONTROL_POINT_CAPTURES -> objectives.controlPointCaptures
            ScoreType.HIGHEST_KILLSTREAK -> killstreaks[killstreaks.keys.maxOrNull() ?: 100] ?: 0
        }
    }
}

@Serializable
data class PlayerRecords(
    var longestSession: Session? = null,
    var longestProjectileKill: ProjectileRecord? = null,
    var fastestWoolCapture: LongRecord? = null,
    var fastestFlagCapture: LongRecord? = null,
    var fastestFirstBlood: FirstBloodRecord? = null,
    var killsInMatch: IntRecord? = null,
    var deathsInMatch: IntRecord? = null,
)

@Serializable
data class ProjectileRecord(val matchId: String, val player: SimplePlayer, val distance: Int)

@Serializable
data class FirstBloodRecord(val matchId: String, val attacker: SimplePlayer, val victim: SimplePlayer, val time: Long)

@Serializable
data class IntRecord(val matchId: String, val player: SimplePlayer, val value: Int)

@Serializable
data class LongRecord(val matchId: String, val player: SimplePlayer, val value: Long)

@Serializable
data class PlayerObjectiveStatistics(
    var coreLeaks: Int = 0,
    var coreBlockDestroys: Int = 0,
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
data class PlayerMessages(var staff: Int = 0, var global: Int = 0, var team: Int = 0) {
    val total: Int
        get() = staff + global + team
}