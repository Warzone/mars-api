package network.warzone.api.socket.map

import network.warzone.api.database.MatchCache
import network.warzone.api.database.models.*
import network.warzone.api.socket.match.MatchEndData
import network.warzone.api.socket.participant.ParticipantContext
import network.warzone.api.socket.participant.PlayerMatchResult
import network.warzone.api.socket.player.PlayerDeathData
import network.warzone.api.socket.player.PlayerListener
import java.util.*

// todo: separate from PlayerListener, ideally a new MatchListener system
object MapRecordListener : PlayerListener<ParticipantContext>() {
    override suspend fun onKill(
        context: ParticipantContext,
        data: PlayerDeathData,
        firstBlood: Boolean
    ): ParticipantContext {
        val map = context.match.level

        if (firstBlood) {
            // How long the match has been in progress before the first blood
            val time = Date().time - context.match.startedAt!!

            val recordTime = map.records.fastestFirstBlood?.time
            if (recordTime == null || time < recordTime)  // The record was beat (or set)
                map.records.fastestFirstBlood =
                    FirstBloodRecord(context.match._id, context.profile.simplePlayer, data.victim, time)
        }

        if (data.distance != null && context.match.participants.size >= 6 && data.cause != DamageCause.FALL) {
            val recordDistance = map.records.longestProjectileKill?.distance
            if (recordDistance == null || data.distance > recordDistance)
                map.records.longestProjectileKill =
                    ProjectileRecord(context.match._id, context.profile.simplePlayer, data.distance)
        }

        MatchCache.set(context.match._id, context.match)
        return context
    }

    override suspend fun onWoolPlace(context: ParticipantContext, heldTime: Long): ParticipantContext {
        val map = context.match.level

        val recordTime = map.records.fastestWoolCapture?.value
        if (recordTime == null || heldTime < recordTime) map.records.fastestWoolCapture =
            LongRecord(context.match._id, context.profile.simplePlayer, heldTime)

        MatchCache.set(context.match._id, context.match)
        return context
    }

    override suspend fun onFlagPlace(context: ParticipantContext, heldTime: Long): ParticipantContext {
        val map = context.match.level

        val recordTime = map.records.fastestFlagCapture?.value
        if (recordTime == null || heldTime < recordTime) map.records.fastestFlagCapture =
            LongRecord(context.match._id, context.profile.simplePlayer, heldTime)

        MatchCache.set(context.match._id, context.match)
        return context
    }

    override suspend fun onKillstreak(context: ParticipantContext, amount: Int): ParticipantContext {
        val map = context.match.level

        val recordKillstreak = map.records.highestKillstreak
        if (amount > (recordKillstreak?.value ?: 0)) map.records.highestKillstreak =
            IntRecord(context.match._id, context.profile.simplePlayer, amount)

        MatchCache.set(context.match._id, context.match)
        return context
    }

    override suspend fun onMatchEnd(
        context: ParticipantContext,
        data: MatchEndData,
        bigStats: MatchEndData.BigStats,
        result: PlayerMatchResult
    ): ParticipantContext {
        val map = context.match.level

        val kills = context.profile.stats.kills
        val recordKills = map.records.killsInMatch?.value ?: 0
        if (kills > recordKills) map.records.killsInMatch =
            IntRecord(context.match._id, context.profile.simplePlayer, kills)

        val deaths = context.profile.stats.deaths
        val recordDeaths = map.records.deathsInMatch?.value ?: 0
        if (deaths > recordDeaths) map.records.deathsInMatch =
            IntRecord(context.match._id, context.profile.simplePlayer, deaths)

        MatchCache.set(context.match._id, context.match)
        return context
    }
}