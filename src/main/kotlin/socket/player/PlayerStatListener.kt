package network.warzone.api.socket.player

import network.warzone.api.database.models.DamageCause
import network.warzone.api.database.models.DestroyableGoal
import network.warzone.api.socket.match.MatchEndData
import network.warzone.api.socket.participant.PlayerMatchResult
import network.warzone.api.socket.participant.PlayerMatchResult.*
import kotlin.math.max
import kotlin.math.min

object PlayerStatListener : PlayerListener<PlayerContext>() {
    override suspend fun onDeath(
        context: PlayerContext,
        data: PlayerDeathData,
        firstBlood: Boolean
    ): PlayerContext {
        if (!context.isTrackingStats) return context
        val (profile) = context

        profile.stats.deaths++

        if (data.cause == DamageCause.VOID) profile.stats.voidDeaths++

        if (firstBlood) profile.stats.firstBloodsSuffered++

        if (data.isMurder) {
            val weaponDeaths = profile.stats.weaponDeaths[data.safeWeapon] ?: 0
            profile.stats.weaponDeaths[data.safeWeapon] = weaponDeaths + 1
        }

        return context
    }

    override suspend fun onKill(
        context: PlayerContext,
        data: PlayerDeathData,
        firstBlood: Boolean
    ): PlayerContext {
        if (!context.isTrackingStats) return context
        val (profile) = context

        profile.stats.kills++

        if (firstBlood) profile.stats.firstBloods++
        if (data.cause == DamageCause.VOID) profile.stats.voidKills++

        val weaponKills = profile.stats.weaponKills[data.safeWeapon] ?: 0
        profile.stats.weaponKills[data.safeWeapon] = weaponKills + 1

        return context
    }

    override suspend fun onKillstreak(context: PlayerContext, amount: Int): PlayerContext {
        if (!context.isTrackingStats) return context
        val (profile) = context
        val prevAmount = profile.stats.killstreaks[amount] ?: 0
        profile.stats.killstreaks[amount] = prevAmount + 1
        return context
    }

    override suspend fun onKillstreakEnd(context: PlayerContext, amount: Int): PlayerContext {
        if (!context.isTrackingStats) return context
        val (profile) = context
        val prevAmount = profile.stats.killstreaksEnded[amount] ?: 0
        profile.stats.killstreaksEnded[amount] = prevAmount + 1
        return context
    }

    override suspend fun onCoreLeak(
        context: PlayerContext,
        percentage: Float,
        blockCount: Int
    ): PlayerContext {
        if (!context.isTrackingStats) return context
        context.profile.stats.objectives.coreLeaks++
        context.profile.stats.objectives.coreBlockDestroys += blockCount
        return context
    }

    override suspend fun onControlPointCapture(context: PlayerContext, contributors: Int): PlayerContext {
        if (!context.isTrackingStats) return context
        context.profile.stats.objectives.controlPointCaptures++
        return context
    }

    override suspend fun onDestroyableDamage(
        context: PlayerContext,
        destroyable: DestroyableGoal,
        blockCount: Int
    ): PlayerContext {
        if (!context.isTrackingStats) return context
        context.profile.stats.objectives.destroyableBlockDestroys += blockCount
        return context
    }

    override suspend fun onDestroyableDestroy(
        context: PlayerContext,
        percentage: Float,
        blockCount: Int
    ): PlayerContext {
        if (!context.isTrackingStats) return context
        context.profile.stats.objectives.destroyableDestroys++
        return context
    }

    override suspend fun onFlagPlace(context: PlayerContext, heldTime: Long): PlayerContext {
        if (!context.isTrackingStats) return context
        context.profile.stats.objectives.flagCaptures++
        context.profile.stats.objectives.totalFlagHoldTime += heldTime
        return context
    }

    override suspend fun onFlagPickup(context: PlayerContext): PlayerContext {
        if (!context.isTrackingStats) return context
        context.profile.stats.objectives.flagPickups++
        return context
    }

    override suspend fun onFlagDrop(context: PlayerContext, heldTime: Long): PlayerContext {
        if (!context.isTrackingStats) return context
        context.profile.stats.objectives.flagDrops++
        context.profile.stats.objectives.totalFlagHoldTime += heldTime
        return context
    }

    override suspend fun onFlagDefend(context: PlayerContext): PlayerContext {
        if (!context.isTrackingStats) return context
        context.profile.stats.objectives.flagDefends++
        return context
    }

    override suspend fun onWoolPlace(context: PlayerContext, heldTime: Long): PlayerContext {
        if (!context.isTrackingStats) return context
        context.profile.stats.objectives.woolCaptures++
        return context
    }

    override suspend fun onWoolPickup(context: PlayerContext): PlayerContext {
        if (!context.isTrackingStats) return context
        context.profile.stats.objectives.woolPickups++
        return context
    }

    override suspend fun onWoolDrop(context: PlayerContext, heldTime: Long): PlayerContext {
        if (!context.isTrackingStats) return context
        context.profile.stats.objectives.woolDrops++
        return context
    }

    override suspend fun onWoolDefend(context: PlayerContext): PlayerContext {
        if (!context.isTrackingStats) return context
        context.profile.stats.objectives.woolDefends++
        return context
    }

    override suspend fun onChat(context: PlayerContext, data: PlayerChatData): PlayerContext {
        if (!context.isTrackingStats) return context
        when (data.channel) {
            PlayerChatData.ChatChannel.GLOBAL -> context.profile.stats.messages.global++
            PlayerChatData.ChatChannel.TEAM -> context.profile.stats.messages.team++
            PlayerChatData.ChatChannel.STAFF -> context.profile.stats.messages.staff++
        }
        return context
    }

    override suspend fun onMatchEnd(
        context: PlayerContext,
        data: MatchEndData,
        bigStats: MatchEndData.BigStats,
        result: PlayerMatchResult
    ): PlayerContext {
        if (!context.isTrackingStats) {
            context.sendMessage("&aYour stats were not affected by this match since stat tracking is disabled, however you still gained XP.")
            return context
        }
        val (profile) = context

        val blocks = bigStats.blocks
        blocks.blocksBroken.forEach { interaction ->
            val block = interaction.key
            profile.stats.blocksBroken[block] = interaction.value
        }
        blocks.blocksPlaced.forEach { interaction ->
            val block = interaction.key
            profile.stats.blocksPlaced[block] = interaction.value
        }

        profile.stats.bowShotsTaken += bigStats.bowShotsTaken
        profile.stats.bowShotsHit += bigStats.bowShotsHit
        profile.stats.damageGiven += bigStats.damageGiven
        profile.stats.damageTaken += bigStats.damageTaken
        profile.stats.damageGivenBow += bigStats.damageGivenBow

        val (participant) = context.getParticipant()

        // min ( 10% of match length | 1 minute )
        val minimumPlaytime = min(0.10 * context.match.length, 60000.0)
        val isPlaying = participant.partyName != null

        if (participant.stats.gamePlaytime > minimumPlaytime) {
            when (result) {
                TIE -> profile.stats.ties++
                WIN -> profile.stats.wins++
                LOSE -> profile.stats.losses++
                else -> Unit
            }
        } else context.sendMessage(
            "&cYour stats were not affected by the outcome of this match as you did not participate for long enough."
        )

        val timeElapsedBeforeJoining = max(participant.firstJoinedMatchAt - context.match.startedAt!!, 0)
        val wasPresentAtStart = timeElapsedBeforeJoining < minimumPlaytime

        if (participant.stats.gamePlaytime > minimumPlaytime) profile.stats.matches++
        if (wasPresentAtStart) profile.stats.matchesPresentStart++
        if (participant.stats.timeAway < 20000 && isPlaying) profile.stats.matchesPresentFull++
        if (isPlaying) profile.stats.matchesPresentEnd++

        profile.stats.gamePlaytime += participant.stats.gamePlaytime

        return context
    }
}