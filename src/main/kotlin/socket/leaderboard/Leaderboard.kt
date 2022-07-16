package network.warzone.api.socket.leaderboard

import kotlinx.serialization.Serializable
import network.warzone.api.database.Database
import network.warzone.api.database.Redis
import java.time.Month
import java.util.*

fun getCalendar(): Calendar {
    return Calendar.getInstance(TimeZone.getTimeZone("EST")).also { it.add(Calendar.HOUR_OF_DAY, 7) }
}

enum class Season {
    SPRING,
    SUMMER,
    AUTUMN,
    WINTER;

    /**
     * This follows Northern Hemisphere climate
     */
    companion object {
        fun ofNorthern(month: Month): Season {
            return when (month) {
                Month.MARCH, Month.APRIL -> SPRING
                Month.MAY, Month.JUNE, Month.JULY, Month.AUGUST -> SUMMER
                Month.SEPTEMBER, Month.OCTOBER -> AUTUMN
                Month.NOVEMBER, Month.DECEMBER, Month.JANUARY, Month.FEBRUARY -> WINTER
            }
        }
    }
}

enum class LeaderboardPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    SEASONALLY,
    YEARLY,
    ALL_TIME;

    fun getTodayId(): String {
        val cal = getCalendar()
        when (this) {
            DAILY -> {
                val date = cal.get(Calendar.DAY_OF_MONTH)
                val month = cal.get(Calendar.MONTH)
                val year = cal.get(Calendar.YEAR)
                return "$year:d:$month:$date"
            }
            WEEKLY -> {
                val week = cal.get(Calendar.WEEK_OF_YEAR)
                val year = cal.get(Calendar.YEAR)
                return "$year:w:$week"
            }
            MONTHLY -> {
                val month = cal.get(Calendar.MONTH)
                val year = cal.get(Calendar.YEAR)
                return "$year:m:$month"
            }
            SEASONALLY -> {
                val month = cal.get(Calendar.MONTH)
                val season = Season.ofNorthern(Month.of(month + 1)).toString().lowercase()
                val year = cal.get(Calendar.YEAR)
                return "$year:s:$season"
            }
            YEARLY -> {
                val year = cal.get(Calendar.YEAR)
                return "$year:y"
            }
            ALL_TIME -> {
                return "all"
            }
        }
    }

    companion object {
        fun find(name: String): LeaderboardPeriod? {
            return try {
                valueOf(name.uppercase())
            } catch (e: Exception) {
                null
            }
        }
    }
}

enum class ScoreType {
    KILLS,
    DEATHS,
    FIRST_BLOODS,
    WINS,
    LOSSES,
    TIES,
    XP,
    MESSAGES_SENT,
    MATCHES_PLAYED,
    SERVER_PLAYTIME,
    GAME_PLAYTIME,
    CORE_LEAKS,
    CORE_BLOCK_DESTROYS,
    DESTROYABLE_DESTROYS,
    DESTROYABLE_BLOCK_DESTROYS,
    FLAG_CAPTURES,
    FLAG_DROPS,
    FLAG_PICKUPS,
    FLAG_DEFENDS,
    FLAG_HOLD_TIME,
    WOOL_CAPTURES,
    WOOL_DROPS,
    WOOL_PICKUPS,
    WOOL_DEFENDS,
    CONTROL_POINT_CAPTURES,
    HIGHEST_KILLSTREAK;

    fun toLeaderboard(): Leaderboard {
        return when (this) {
            KILLS -> KillsLeaderboard
            DEATHS -> DeathsLeaderboard
            FIRST_BLOODS -> FirstBloodsLeaderboard
            WINS -> WinsLeaderboard
            LOSSES -> LossesLeaderboard
            TIES -> TiesLeaderboard
            XP -> XPLeaderboard
            MESSAGES_SENT -> MessagesSentLeaderboard
            MATCHES_PLAYED -> MatchesPlayedLeaderboard
            SERVER_PLAYTIME -> ServerPlaytimeLeaderboard
            GAME_PLAYTIME -> GamePlaytimeLeaderboard

            CORE_LEAKS -> CoreLeaksLeaderboard
            CORE_BLOCK_DESTROYS -> CoreBlockDestroysLeaderboard
            DESTROYABLE_DESTROYS -> DestroyableDestroysLeaderboard
            DESTROYABLE_BLOCK_DESTROYS -> DestroyableBlockDestroysLeaderboard
            FLAG_CAPTURES -> FlagCapturesLeaderboard
            FLAG_PICKUPS -> FlagPickupsLeaderboard
            FLAG_DEFENDS -> FlagDefendsLeaderboard
            FLAG_DROPS -> FlagDropsLeaderboard
            FLAG_HOLD_TIME -> FlagHoldTimeLeaderboard
            WOOL_CAPTURES -> WoolCapturesLeaderboard
            WOOL_DROPS -> WoolDropsLeaderboard
            WOOL_DEFENDS -> WoolDefendsLeaderboard
            WOOL_PICKUPS -> WoolPickupsLeaderboard
            CONTROL_POINT_CAPTURES -> ControlPointCapturesLeaderboard
            HIGHEST_KILLSTREAK -> HighestKillstreakLeaderboard
        }
    }

    companion object {
        fun find(name: String): ScoreType? {
            return try {
                valueOf(name.uppercase())
            } catch (e: Exception) {
                null
            }
        }
    }
}

object KillsLeaderboard : Leaderboard(ScoreType.KILLS)
object DeathsLeaderboard : Leaderboard(ScoreType.DEATHS)
object FirstBloodsLeaderboard : Leaderboard(ScoreType.FIRST_BLOODS)
object WinsLeaderboard : Leaderboard(ScoreType.WINS)
object LossesLeaderboard : Leaderboard(ScoreType.LOSSES)
object TiesLeaderboard : Leaderboard(ScoreType.TIES)
object XPLeaderboard : Leaderboard(ScoreType.XP)
object MessagesSentLeaderboard : Leaderboard(ScoreType.MESSAGES_SENT)
object MatchesPlayedLeaderboard : Leaderboard(ScoreType.MATCHES_PLAYED)

object CoreLeaksLeaderboard : Leaderboard(ScoreType.CORE_LEAKS)
object CoreBlockDestroysLeaderboard : Leaderboard(ScoreType.CORE_BLOCK_DESTROYS)

object DestroyableDestroysLeaderboard : Leaderboard(ScoreType.DESTROYABLE_DESTROYS)
object DestroyableBlockDestroysLeaderboard : Leaderboard(ScoreType.DESTROYABLE_BLOCK_DESTROYS)

object FlagCapturesLeaderboard : Leaderboard(ScoreType.FLAG_CAPTURES)
object FlagPickupsLeaderboard : Leaderboard(ScoreType.FLAG_PICKUPS)
object FlagDropsLeaderboard : Leaderboard(ScoreType.FLAG_DROPS)
object FlagDefendsLeaderboard : Leaderboard(ScoreType.FLAG_DEFENDS)
object FlagHoldTimeLeaderboard : Leaderboard(ScoreType.FLAG_HOLD_TIME)

object WoolCapturesLeaderboard : Leaderboard(ScoreType.WOOL_CAPTURES)
object WoolPickupsLeaderboard : Leaderboard(ScoreType.WOOL_PICKUPS)
object WoolDropsLeaderboard : Leaderboard(ScoreType.WOOL_DROPS)
object WoolDefendsLeaderboard : Leaderboard(ScoreType.WOOL_DEFENDS)

object ControlPointCapturesLeaderboard : Leaderboard(ScoreType.CONTROL_POINT_CAPTURES)

object HighestKillstreakLeaderboard : Leaderboard(ScoreType.HIGHEST_KILLSTREAK)

// If these leaderboards are still being used in 2037, make sure to switch to Longs before 2038
object ServerPlaytimeLeaderboard : Leaderboard(ScoreType.SERVER_PLAYTIME)
object GamePlaytimeLeaderboard : Leaderboard(ScoreType.GAME_PLAYTIME)

abstract class Leaderboard(val type: ScoreType) {
    fun flushAllTime() {
        Redis.pool.resource.use {
            it.del(getId(LeaderboardPeriod.ALL_TIME))
        }
    }

    /**
     * Populates the all-time leaderboard directly from Mongo data.
     * If there is data in memory already, this method will just add onto it.
     * For resetting, use [flushAllTime] before this.
     */
    suspend fun populateAllTime() {
        val players = Database.players.find().toList()
        val sorted = players.sortedByDescending { it.stats.getScore(type) }
        val members = sorted.associateBy({ it.idName }, { it.stats.getScore(type).toDouble() })
        Redis.pool.resource.use {
            it.zadd(getId(LeaderboardPeriod.ALL_TIME), members)
        }
    }

    fun getId(period: LeaderboardPeriod): String {
        return "lb:$type:${period.getTodayId()}"
    }

    /**
     * For a standard player leaderboard, id = Player#idName ("uuid-xx-xx/username")
     */
    fun set(id: String, score: Int) {
        val double = score.toDouble()
        Redis.pool.resource.use { redis ->
            // Set value on every leaderboard period
            LeaderboardPeriod.values().forEach { period ->
                redis.zadd(getId(period), double, id)
            }
        }
    }

    /**
     * For a standard player leaderboard, id = Player#idName ("uuid-xx-xx/username")
     */
    fun increment(id: String, incr: Int = 1) {
        val double = incr.toDouble()
        Redis.pool.resource.use { redis ->
            // Increment on every leaderboard period
            LeaderboardPeriod.values().forEach { period ->
                redis.zincrby(getId(period), double, id)
            }
        }
    }

    fun fetchTop(period: LeaderboardPeriod, limit: Int): List<LeaderboardEntry> {
        Redis.pool.resource.use { redis ->
            val top = redis.zrevrangeWithScores(getId(period), 0, (limit - 1).toLong())
            return top.map {
                val split = it.element.split("/")
                return@map LeaderboardEntry(split.first(), split.last(), it.score.toInt())
            }
        }
    }

    fun setIfHigher(id: String, new: Int) {
        Redis.pool.resource.use { redis ->
            LeaderboardPeriod.values().forEach { period ->
                val key = getId(period)
                val current = redis.zscore(key, id)?.toInt() ?: 0
                if (new > current)
                    redis.zadd(key, new.toDouble(), id)
            }
        }
    }

    fun getPosition(id: String, period: LeaderboardPeriod): Long {
        Redis.pool.resource.use { redis ->
            return redis.zrevrank(getId(period), id) ?: -1
        }
    }
}

@Serializable
data class LeaderboardEntry(val id: String, val name: String, val score: Int)