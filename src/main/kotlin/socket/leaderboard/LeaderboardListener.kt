package network.warzone.api.socket.leaderboard

import network.warzone.api.socket.match.MatchEndData
import network.warzone.api.socket.participant.ParticipantContext
import network.warzone.api.socket.participant.PlayerMatchResult
import network.warzone.api.socket.player.PlayerDeathData
import network.warzone.api.socket.player.PlayerListener

/**
 * Listens to player events and updates leaderboard score.
 * Server playtime is handled in the logout HTTP endpoint.
 * XP is handled in the [PlayerContext]#addXP() method.
 */
object LeaderboardListener : PlayerListener<ParticipantContext>() {
    override suspend fun onKill(context: ParticipantContext, data: PlayerDeathData, firstBlood: Boolean): ParticipantContext {
        KillsLeaderboard.increment(context.profile.idName)
        if (firstBlood) FirstBloodsLeaderboard.increment(context.profile.idName)
        return context
    }

    override suspend fun onDeath(context: ParticipantContext, data: PlayerDeathData, firstBlood: Boolean): ParticipantContext {
        DeathsLeaderboard.increment(context.profile.idName)
        return context
    }

    override suspend fun onMatchEnd(
        context: ParticipantContext,
        data: MatchEndData,
        bigStats: MatchEndData.BigStats,
        result: PlayerMatchResult
    ): ParticipantContext {
        val (profile) = context
        WinsLeaderboard.increment(profile.idName)
        LossesLeaderboard.increment(profile.idName)
        TiesLeaderboard.increment(profile.idName)
        MatchesPlayedLeaderboard.increment(profile.idName)

        // If a player sends messages without playing, their profile stats will be updated but their leaderboard score will only update the next time they play a match
        MessagesSentLeaderboard.increment(profile.idName, profile.stats.messages.total)

        // Will break in 2038
        GamePlaytimeLeaderboard.increment(profile.idName, profile.stats.gamePlaytime.toInt())
        return context
    }

    override suspend fun onCoreLeak(context: ParticipantContext, percentage: Float, blockCount: Int): ParticipantContext {
        CoreLeaksLeaderboard.increment(context.profile.idName)
        CoreBlockDestroysLeaderboard.increment(context.profile.idName, blockCount)
        return context
    }

    override suspend fun onDestroyableDestroy(
        context: ParticipantContext,
        percentage: Float,
        blockCount: Int
    ): ParticipantContext {
        DestroyableDestroysLeaderboard.increment(context.profile.idName)
        DestroyableBlockDestroysLeaderboard.increment(context.profile.idName, blockCount)
        return context
    }

    override suspend fun onFlagPlace(context: ParticipantContext, heldTime: Long): ParticipantContext {
        FlagCapturesLeaderboard.increment(context.profile.idName)
        FlagHoldTimeLeaderboard.increment(context.profile.idName, heldTime.toInt())
        return context
    }

    override suspend fun onFlagPickup(context: ParticipantContext): ParticipantContext {
        FlagPickupsLeaderboard.increment(context.profile.idName)
        return context
    }

    override suspend fun onFlagDrop(context: ParticipantContext, heldTime: Long): ParticipantContext {
        FlagDropsLeaderboard.increment(context.profile.idName)
        FlagHoldTimeLeaderboard.increment(context.profile.idName, heldTime.toInt())
        return context
    }

    override suspend fun onFlagDefend(context: ParticipantContext): ParticipantContext {
        FlagDefendsLeaderboard.increment(context.profile.idName)
        return context
    }

    override suspend fun onWoolPlace(context: ParticipantContext, heldTime: Long): ParticipantContext {
        WoolCapturesLeaderboard.increment(context.profile.idName)
        return context
    }

    override suspend fun onWoolPickup(context: ParticipantContext): ParticipantContext {
        WoolPickupsLeaderboard.increment(context.profile.idName)
        return context
    }

    override suspend fun onWoolDrop(context: ParticipantContext, heldTime: Long): ParticipantContext {
        WoolDropsLeaderboard.increment(context.profile.idName)
        return context
    }

    override suspend fun onWoolDefend(context: ParticipantContext): ParticipantContext {
        WoolDefendsLeaderboard.increment(context.profile.idName)
        return context
    }

    override suspend fun onControlPointCapture(context: ParticipantContext, contributors: Int): ParticipantContext {
        ControlPointCapturesLeaderboard.increment(context.profile.idName)
        return context
    }

    override suspend fun onKillstreak(context: ParticipantContext, amount: Int): ParticipantContext {
        HighestKillstreakLeaderboard.setIfHigher(context.profile.idName, amount)
        return context
    }
}