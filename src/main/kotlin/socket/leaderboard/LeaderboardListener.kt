package network.warzone.api.socket.leaderboard

import network.warzone.api.socket.match.MatchEndData
import network.warzone.api.socket.participant.PlayerMatchResult
import network.warzone.api.socket.player.PlayerContext
import network.warzone.api.socket.player.PlayerDeathData
import network.warzone.api.socket.player.PlayerListener

/**
 * Listens to player events and updates leaderboard score.
 * Server playtime is handled in the logout HTTP endpoint.
 * XP is handled in the [PlayerContext]#addXP() method.
 */
object LeaderboardListener : PlayerListener<PlayerContext>() {
    override suspend fun onKill(context: PlayerContext, data: PlayerDeathData, firstBlood: Boolean): PlayerContext {
        KillsLeaderboard.increment(context.profile.idName)
        if (firstBlood) FirstBloodsLeaderboard.increment(context.profile.idName)
        return context
    }

    override suspend fun onDeath(context: PlayerContext, data: PlayerDeathData, firstBlood: Boolean): PlayerContext {
        DeathsLeaderboard.increment(context.profile.idName)
        return context
    }

    override suspend fun onMatchEnd(
        context: PlayerContext,
        data: MatchEndData,
        bigStats: MatchEndData.BigStats,
        result: PlayerMatchResult
    ): PlayerContext {
        val (profile) = context
        WinsLeaderboard.set(profile.idName, profile.stats.wins)
        LossesLeaderboard.set(profile.idName, profile.stats.losses)
        TiesLeaderboard.set(profile.idName, profile.stats.ties)

        // If a player sends messages without playing, their profile stats will be updated but their leaderboard score will only update the next time they play a match
        MessagesSentLeaderboard.set(profile.idName, profile.stats.messages.total)

        MatchesPlayedLeaderboard.set(profile.idName, profile.stats.matches)

        // Will break in 2038
        GamePlaytimeLeaderboard.set(profile.idName, profile.stats.gamePlaytime.toInt())
        return context
    }

    override suspend fun onCoreLeak(context: PlayerContext, percentage: Float, blockCount: Int): PlayerContext {
        CoreLeaksLeaderboard.increment(context.profile.idName)
        CoreBlockDestroysLeaderboard.increment(context.profile.idName, blockCount)
        return context
    }

    override suspend fun onDestroyableDestroy(
        context: PlayerContext,
        percentage: Float,
        blockCount: Int
    ): PlayerContext {
        DestroyableDestroysLeaderboard.increment(context.profile.idName)
        DestroyableBlockDestroysLeaderboard.increment(context.profile.idName, blockCount)
        return context
    }

    override suspend fun onFlagPlace(context: PlayerContext, heldTime: Long): PlayerContext {
        FlagCapturesLeaderboard.increment(context.profile.idName)
        FlagHoldTimeLeaderboard.increment(context.profile.idName, heldTime.toInt())
        return context
    }

    override suspend fun onFlagPickup(context: PlayerContext): PlayerContext {
        FlagPickupsLeaderboard.increment(context.profile.idName)
        return context
    }

    override suspend fun onFlagDrop(context: PlayerContext, heldTime: Long): PlayerContext {
        FlagDropsLeaderboard.increment(context.profile.idName)
        FlagHoldTimeLeaderboard.increment(context.profile.idName, heldTime.toInt())
        return context
    }

    override suspend fun onFlagDefend(context: PlayerContext): PlayerContext {
        FlagDefendsLeaderboard.increment(context.profile.idName)
        return context
    }

    override suspend fun onWoolPlace(context: PlayerContext, heldTime: Long): PlayerContext {
        WoolCapturesLeaderboard.increment(context.profile.idName)
        return context
    }

    override suspend fun onWoolPickup(context: PlayerContext): PlayerContext {
        WoolPickupsLeaderboard.increment(context.profile.idName)
        return context
    }

    override suspend fun onWoolDrop(context: PlayerContext, heldTime: Long): PlayerContext {
        WoolDropsLeaderboard.increment(context.profile.idName)
        return context
    }

    override suspend fun onWoolDefend(context: PlayerContext): PlayerContext {
        WoolDefendsLeaderboard.increment(context.profile.idName)
        return context
    }

    override suspend fun onControlPointCapture(context: PlayerContext, contributors: Int): PlayerContext {
        ControlPointCapturesLeaderboard.increment(context.profile.idName)
        return context
    }

    override suspend fun onKillstreak(context: PlayerContext, amount: Int): PlayerContext {
        HighestKillstreakLeaderboard.setIfHigher(context.profile.idName, amount)
        return context
    }
}