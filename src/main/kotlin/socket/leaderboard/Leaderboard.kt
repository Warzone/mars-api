package network.warzone.api.socket.leaderboard

import kotlinx.serialization.Serializable
import network.warzone.api.database.Database
import network.warzone.api.database.Redis
import java.time.Month
import java.util.*

fun getCalendar(): Calendar {
    return Calendar.getInstance(TimeZone.getTimeZone("EST")).also { it.add(Calendar.HOUR_OF_DAY, 7) }
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
    GAME_PLAYTIME;

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

    fun getTodayID(): String {
        val cal = getCalendar()
        when (this) {
            DAILY -> {
                val date = cal.get(Calendar.DAY_OF_YEAR)
                val year = cal.get(Calendar.YEAR)
                return "d-$date-$year"
            }
            WEEKLY -> {
                val week = cal.get(Calendar.WEEK_OF_YEAR)
                val year = cal.get(Calendar.YEAR)
                return "w-$week-$year"
            }
            MONTHLY -> {
                val month = cal.get(Calendar.MONTH)
                val year = cal.get(Calendar.YEAR)
                return "m-$month-$year"
            }
            SEASONALLY -> {
                val month = cal.get(Calendar.MONTH)
                val season = Season.ofNorthern(Month.of(month + 1))
                val year = cal.get(Calendar.YEAR)
                return "s-$season-$year"
            }
            YEARLY -> {
                val year = cal.get(Calendar.YEAR)
                return "y-$year"
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

object KillsLeaderboard : Leaderboard(ScoreType.KILLS)
object DeathsLeaderboard : Leaderboard(ScoreType.DEATHS)
object FirstBloodsLeaderboard : Leaderboard(ScoreType.FIRST_BLOODS)
object WinsLeaderboard : Leaderboard(ScoreType.WINS)
object LossesLeaderboard : Leaderboard(ScoreType.LOSSES)
object TiesLeaderboard : Leaderboard(ScoreType.TIES)
object XPLeaderboard : Leaderboard(ScoreType.XP)
object MessagesSentLeaderboard : Leaderboard(ScoreType.MESSAGES_SENT)
object MatchesPlayedLeaderboard : Leaderboard(ScoreType.MATCHES_PLAYED)

// If these leaderboards are still being used in 2037, make sure to switch to Longs before 2038
object ServerPlaytimeLeaderboard : Leaderboard(ScoreType.SERVER_PLAYTIME)
object GamePlaytimeLeaderboard : Leaderboard(ScoreType.GAME_PLAYTIME)

abstract class Leaderboard(private val type: ScoreType) {
    fun flushAllTime() {
        Redis.pool.resource.use {
            it.del(getID(LeaderboardPeriod.ALL_TIME))
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
            it.zadd(getID(LeaderboardPeriod.ALL_TIME), members)
        }
    }

    fun getID(period: LeaderboardPeriod): String {
        return "lb:$type:${period.getTodayID()}"
    }

    /**
     * For a standard player leaderboard, id = Player#idName ("uuid-xx-xx/username")
     */
    fun set(id: String, score: Int) {
        val double = score.toDouble()
        Redis.pool.resource.use {
            it.zadd(getID(LeaderboardPeriod.DAILY), double, id)
            it.zadd(getID(LeaderboardPeriod.WEEKLY), double, id)
            it.zadd(getID(LeaderboardPeriod.MONTHLY), double, id)
            it.zadd(getID(LeaderboardPeriod.SEASONALLY), double, id)
            it.zadd(getID(LeaderboardPeriod.YEARLY), double, id)
            it.zadd(getID(LeaderboardPeriod.ALL_TIME), double, id)
        }
    }

    /**
     * For a standard player leaderboard, id = Player#idName ("uuid-xx-xx/username")
     */
    fun increment(id: String, incr: Int) {
        val double = incr.toDouble()
        Redis.pool.resource.use {
            it.zincrby(getID(LeaderboardPeriod.DAILY), double, id)
            it.zincrby(getID(LeaderboardPeriod.WEEKLY), double, id)
            it.zincrby(getID(LeaderboardPeriod.MONTHLY), double, id)
            it.zincrby(getID(LeaderboardPeriod.SEASONALLY), double, id)
            it.zincrby(getID(LeaderboardPeriod.YEARLY), double, id)
            it.zincrby(getID(LeaderboardPeriod.ALL_TIME), double, id)
        }
    }

    fun fetchTop(period: LeaderboardPeriod, limit: Int): List<LeaderboardEntry> {
        Redis.pool.resource.use { redis ->
            val top = redis.zrevrangeWithScores(getID(period), 0, (limit - 1).toLong())
            return top.map {
                val split = it.element.split("/")
                return@map LeaderboardEntry(split.first(), split.last(), it.score.toInt())
            }
        }
    }
}

@Serializable
data class LeaderboardEntry(val id: String, val name: String, val score: Int)