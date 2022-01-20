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
        KillsLeaderboard.increment(context.profile.idName, 1)
        if (firstBlood) FirstBloodsLeaderboard.increment(context.profile.idName, 1)
        return context
    }

    override suspend fun onDeath(context: PlayerContext, data: PlayerDeathData, firstBlood: Boolean): PlayerContext {
        DeathsLeaderboard.increment(context.profile.idName, 1)
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

        MessagesSentLeaderboard.set(profile.idName, profile.stats.messages.total)

        MatchesPlayedLeaderboard.set(profile.idName, profile.stats.matches)

        // Will break in 2038
        GamePlaytimeLeaderboard.set(profile.idName, profile.stats.gamePlaytime.toInt())
        return context
    }
}