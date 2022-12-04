package network.warzone.api.socket.player

import network.warzone.api.database.models.DestroyableGoal
import network.warzone.api.socket.match.MatchEndData
import network.warzone.api.socket.participant.PlayerMatchResult
import kotlin.math.max
import kotlin.math.min

const val XP_PER_LEVEL = 5000
const val XP_BEGINNER_ASSIST_MAX = 10

const val XP_WIN = 200
const val XP_LOSS = 100
const val XP_DRAW = 150
const val XP_KILL = 40
const val XP_DEATH = 1
const val XP_FIRST_BLOOD = 7
const val XP_WOOL_OBJECTIVE = 60
const val XP_FLAG_OBJECTIVE = 150
const val XP_FLAG_TIME_BONUS = 100 // flag capture bonus xp: 100 - (seconds held before capturing)
const val XP_POINT_CAPTURE_MAX = 100
const val XP_DESTROYABLE_WHOLE = 200
const val XP_KILLSTREAK_COEFFICIENT = 10
const val XP_KILLSTREAK_END_COEFFICIENT = 2

fun gain(xp: Int, level: Int): Int {
    val startMultiplier = max(XP_BEGINNER_ASSIST_MAX - level, 1)
    return xp * startMultiplier
}

object PlayerXPListener : PlayerListener<PlayerContext>() {
    override suspend fun onKill(context: PlayerContext, data: PlayerDeathData, firstBlood: Boolean): PlayerContext {
        context.addXP(XP_KILL, "Kill")
        if (firstBlood) context.addXP(XP_FIRST_BLOOD, "First blood")
        return context
    }

    override suspend fun onDeath(context: PlayerContext, data: PlayerDeathData, firstBlood: Boolean): PlayerContext {
        context.addXP(XP_DEATH, "Death", notify = false)
        return context
    }

    override suspend fun onWoolPlace(context: PlayerContext, heldTime: Long): PlayerContext {
        context.addXP(XP_WOOL_OBJECTIVE, "Captured wool")
        return context
    }

    override suspend fun onWoolPickup(context: PlayerContext): PlayerContext {
        context.addXP(XP_WOOL_OBJECTIVE, "Picked up wool")
        return context
    }

    override suspend fun onWoolDefend(context: PlayerContext): PlayerContext {
        context.addXP(XP_WOOL_OBJECTIVE, "Defended wool")
        return context
    }

    override suspend fun onFlagPlace(context: PlayerContext, heldTime: Long): PlayerContext {
        val xp = XP_FLAG_OBJECTIVE + (XP_FLAG_TIME_BONUS - (heldTime / 1000).toInt())
        context.addXP(xp, "Captured flag")
        return context
    }

    override suspend fun onFlagPickup(context: PlayerContext): PlayerContext {
        context.addXP(XP_FLAG_OBJECTIVE, "Picked up flag")
        return context
    }

    override suspend fun onFlagDefend(context: PlayerContext): PlayerContext {
        context.addXP(XP_FLAG_OBJECTIVE, "Defended flag")
        return context
    }

    override suspend fun onKillstreak(context: PlayerContext, amount: Int): PlayerContext {
        context.addXP(XP_KILLSTREAK_COEFFICIENT * amount, "Killstreak x$amount")
        return context
    }

    override suspend fun onControlPointCapture(context: PlayerContext, contributors: Int): PlayerContext {
        val others = contributors - 1
        val xp = max(XP_POINT_CAPTURE_MAX - (others * 10), 20)
        context.addXP(xp, "Captured point")
        return context
    }

    override suspend fun onDestroyableDamage(
        context: PlayerContext,
        destroyable: DestroyableGoal,
        blockCount: Int
    ): PlayerContext {
        val xp = (XP_DESTROYABLE_WHOLE / destroyable.breaksRequired) * blockCount
        context.addXP(xp, "Damaged objective")
        return context
    }

    override suspend fun onCoreLeak(context: PlayerContext, percentage: Float, blockCount: Int): PlayerContext {
        val xp = percentage * XP_DESTROYABLE_WHOLE
        context.addXP(xp.toInt(), "Leaked core")
        return context
    }

    override suspend fun onMatchEnd(
        context: PlayerContext,
        data: MatchEndData,
        bigStats: MatchEndData.BigStats,
        result: PlayerMatchResult
    ): PlayerContext {
        // min ( 10% of match length | 1 minute )
        val minimumPlaytime = min(0.10 * context.match.length, 60000.0)
        val eligibleForResultXP =
            context.match.participants.values.any { it.id == context.profile._id && it.stats.gamePlaytime > minimumPlaytime }

        if (!eligibleForResultXP) return context

        when (result) {
            PlayerMatchResult.WIN -> {
                context.addXP(XP_WIN, "Victory", notify = true, rawOnly = true)
            }
            PlayerMatchResult.LOSE -> {
                context.addXP(XP_LOSS, "Defeat", notify = true, rawOnly = true)
            }
            PlayerMatchResult.TIE -> {
                context.addXP(XP_DRAW, "Tie", notify = true, rawOnly = true)
            }
            else -> Unit
        }

        return context
    }
}
