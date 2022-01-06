package network.warzone.api.socket.player

import network.warzone.api.database.models.FirstBloodRecord
import network.warzone.api.database.models.IntRecord
import network.warzone.api.database.models.LongRecord
import network.warzone.api.database.models.ProjectileRecord
import network.warzone.api.socket.match.MatchEndData
import network.warzone.api.socket.participant.PlayerMatchResult
import java.util.*

object PlayerRecordListener : PlayerListener<PlayerContext>() {
    override suspend fun onKill(context: PlayerContext, data: PlayerDeathData, firstBlood: Boolean): PlayerContext {
        val (profile) = context
        if (firstBlood) {
            // How long the match has been in progress before the first blood
            val time = Date().time - context.match.startedAt!!

            val recordTime = profile.stats.records.fastestFirstBlood?.time
            if (recordTime == null || time < recordTime)  // The record was beat (or set)
                profile.stats.records.fastestFirstBlood = FirstBloodRecord(context.match._id, data.simpleVictim, time)

        }

        if (data.distance != null) {
            val recordDistance = profile.stats.records.longestProjectileKill?.distance
            if (recordDistance == null || data.distance > recordDistance)
                profile.stats.records.longestProjectileKill = ProjectileRecord(context.match._id, data.distance)
        }

        return context
    }

    override suspend fun onWoolPlace(context: PlayerContext, heldTime: Long): PlayerContext {
        val (profile) = context

        val recordTime = profile.stats.records.fastestWoolCapture?.value
        if (recordTime == null || heldTime < recordTime) profile.stats.records.fastestWoolCapture =
            LongRecord(context.match._id, heldTime)

        return context
    }

    override suspend fun onFlagPlace(context: PlayerContext, heldTime: Long): PlayerContext {
        val (profile) = context

        val recordTime = profile.stats.records.fastestFlagCapture?.value
        if (recordTime == null || heldTime < recordTime) profile.stats.records.fastestFlagCapture =
            LongRecord(context.match._id, heldTime)

        return context
    }

    override suspend fun onMatchEnd(
        context: PlayerContext,
        data: MatchEndData,
        bigStats: MatchEndData.BigStats,
        result: PlayerMatchResult
    ): PlayerContext {
        val (profile) = context
        val participant = context.getParticipant().profile

        val kills = participant.stats.kills
        val recordKills = profile.stats.records.killsInMatch?.value ?: 0
        if (kills > recordKills) profile.stats.records.killsInMatch = IntRecord(context.match._id, kills)

        val deaths = participant.stats.deaths
        val recordDeaths = profile.stats.records.deathsInMatch?.value ?: 0
        if (deaths > recordDeaths) profile.stats.records.deathsInMatch = IntRecord(context.match._id, deaths)

        return context
    }
}