package network.warzone.api.socket.player

import network.warzone.api.database.models.*
import network.warzone.api.socket.match.MatchEndData
import network.warzone.api.socket.participant.PlayerMatchResult
import java.util.*

object PlayerRecordListener : PlayerListener<PlayerContext>() {
    override suspend fun onKill(context: PlayerContext, data: PlayerDeathData, firstBlood: Boolean): PlayerContext {
        if (!context.isTrackingStats) return context
        val (profile) = context
        if (firstBlood) {
            // How long the match has been in progress before the first blood
            val time = Date().time - context.match.startedAt!!

            val recordTime = profile.stats.records.fastestFirstBlood?.time
            if (recordTime == null || time < recordTime)  // The record was beat (or set)
                profile.stats.records.fastestFirstBlood =
                    FirstBloodRecord(context.match._id, profile.simple, data.victim, time)

        }

        if (data.distance != null && context.match.participants.size >= 6 && data.cause != DamageCause.FALL) {
            val recordDistance = profile.stats.records.longestProjectileKill?.distance
            if (recordDistance == null || data.distance > recordDistance)
                profile.stats.records.longestProjectileKill = ProjectileRecord(context.match._id, context.profile.simple, data.distance)
        }

        return context
    }

    override suspend fun onWoolPlace(context: PlayerContext, heldTime: Long): PlayerContext {
        if (!context.isTrackingStats) return context
        val (profile) = context

        val recordTime = profile.stats.records.fastestWoolCapture?.value
        if (recordTime == null || heldTime < recordTime) profile.stats.records.fastestWoolCapture =
            LongRecord(context.match._id, context.profile.simple, heldTime)

        return context
    }

    override suspend fun onFlagPlace(context: PlayerContext, heldTime: Long): PlayerContext {
        if (!context.isTrackingStats) return context
        val (profile) = context

        val recordTime = profile.stats.records.fastestFlagCapture?.value
        if (recordTime == null || heldTime < recordTime) profile.stats.records.fastestFlagCapture =
            LongRecord(context.match._id, context.profile.simple, heldTime)

        return context
    }

    override suspend fun onMatchEnd(
        context: PlayerContext,
        data: MatchEndData,
        bigStats: MatchEndData.BigStats,
        result: PlayerMatchResult
    ): PlayerContext {
        if (!context.isTrackingStats) return context
        val (profile) = context
        val participant = context.getParticipant().profile

        val kills = participant.stats.kills
        val recordKills = profile.stats.records.killsInMatch?.value ?: 0
        if (kills > recordKills) profile.stats.records.killsInMatch = IntRecord(context.match._id, context.profile.simple, kills)

        val deaths = participant.stats.deaths
        val recordDeaths = profile.stats.records.deathsInMatch?.value ?: 0
        if (deaths > recordDeaths) profile.stats.records.deathsInMatch = IntRecord(context.match._id, context.profile.simple, deaths)

        return context
    }
}