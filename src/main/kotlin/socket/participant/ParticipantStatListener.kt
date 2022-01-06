package network.warzone.api.socket.participant

import network.warzone.api.database.models.DamageCause
import network.warzone.api.database.models.Duel
import network.warzone.api.socket.match.MatchEndData
import network.warzone.api.socket.player.PlayerChatData
import network.warzone.api.socket.player.PlayerDeathData
import network.warzone.api.socket.player.PlayerListener
import java.util.*

class ParticipantStatListener : PlayerListener<ParticipantContext>() {
    override suspend fun onPartyJoin(context: ParticipantContext, partyName: String): ParticipantContext {
        val (profile) = context

        if (profile.lastLeftPartyAt != null) { // Returning player
            val timeAway = Date().time - profile.lastLeftPartyAt!!
            profile.stats.timeAway += timeAway
        }

        return context
    }

    override suspend fun onPartyLeave(context: ParticipantContext): ParticipantContext {
        val (profile) = context
        profile.stats.gamePlaytime += Date().time - profile.joinedPartyAt!!
        return context
    }

    override suspend fun onDeath(
        context: ParticipantContext,
        data: PlayerDeathData,
        firstBlood: Boolean
    ): ParticipantContext {
        val (profile) = context

        profile.stats.deaths++

        if (data.cause == DamageCause.VOID) profile.stats.voidDeaths++

        if (data.isMurder) {
            val weaponName = data.weapon ?: "NONE"
            val weaponDeaths = profile.stats.weaponDeaths[weaponName] ?: 0
            profile.stats.weaponDeaths[weaponName] = weaponDeaths + 1

            val duel = profile.stats.duels[data.attackerId] ?: Duel()
            duel.deaths++
            profile.stats.duels[data.attackerId!!] = duel
        }

        return context
    }

    override suspend fun onKill(
        context: ParticipantContext,
        data: PlayerDeathData,
        firstBlood: Boolean
    ): ParticipantContext {
        val (profile) = context

        profile.stats.kills++

        val weaponName = data.weapon ?: "NONE"
        val weaponKills = profile.stats.weaponKills[weaponName] ?: 0
        profile.stats.weaponKills[weaponName] = weaponKills + 1

        val duel = profile.stats.duels[data.victimId] ?: Duel()
        duel.kills++
        profile.stats.duels[data.victimId] = duel

        if (data.cause == DamageCause.VOID) profile.stats.voidKills++

        return context
    }

    override suspend fun onKillstreak(context: ParticipantContext, amount: Int): ParticipantContext {
        val (profile) = context
        val prevAmount = profile.stats.killstreaks[amount] ?: 0
        profile.stats.killstreaks[amount] = prevAmount + 1
        return context
    }

    override suspend fun onCoreLeak(
        context: ParticipantContext,
        percentage: Float,
        blockCount: Int
    ): ParticipantContext {
        context.profile.stats.objectives.coreLeaks++
        context.profile.stats.objectives.coreBlockDestroys += blockCount
        return context
    }

    override suspend fun onControlPointCapture(context: ParticipantContext, contributors: Int): ParticipantContext {
        context.profile.stats.objectives.controlPointCaptures++
        return context
    }

    override suspend fun onDestroyableDestroy(
        context: ParticipantContext,
        percentage: Float,
        blockCount: Int
    ): ParticipantContext {
        context.profile.stats.objectives.destroyableDestroys++
        context.profile.stats.objectives.destroyableBlockDestroys += blockCount
        return context
    }

    override suspend fun onFlagPlace(context: ParticipantContext, heldTime: Long): ParticipantContext {
        context.profile.stats.objectives.flagCaptures++
        context.profile.stats.objectives.totalFlagHoldTime += heldTime
        return context
    }

    override suspend fun onFlagPickup(context: ParticipantContext): ParticipantContext {
        context.profile.stats.objectives.flagPickups++
        return context
    }

    override suspend fun onFlagDrop(context: ParticipantContext, heldTime: Long): ParticipantContext {
        context.profile.stats.objectives.flagDrops++
        context.profile.stats.objectives.totalFlagHoldTime += heldTime
        return context
    }

    override suspend fun onFlagDefend(context: ParticipantContext): ParticipantContext {
        context.profile.stats.objectives.flagDefends++
        return context
    }

    override suspend fun onWoolPlace(context: ParticipantContext, heldTime: Long): ParticipantContext {
        context.profile.stats.objectives.woolCaptures++
        return context
    }

    override suspend fun onWoolPickup(context: ParticipantContext): ParticipantContext {
        context.profile.stats.objectives.woolPickups++
        return context
    }

    override suspend fun onWoolDrop(context: ParticipantContext, heldTime: Long): ParticipantContext {
        context.profile.stats.objectives.woolDrops++
        return context
    }

    override suspend fun onWoolDefend(context: ParticipantContext): ParticipantContext {
        context.profile.stats.objectives.woolDefends++
        return context
    }

    override suspend fun onChat(context: ParticipantContext, data: PlayerChatData): ParticipantContext {
        when (data.channel) {
            PlayerChatData.ChatChannel.GLOBAL -> context.profile.stats.messages.global++
            PlayerChatData.ChatChannel.TEAM -> context.profile.stats.messages.team++
            PlayerChatData.ChatChannel.STAFF -> context.profile.stats.messages.staff++
        }
        return context
    }

    override suspend fun onMatchEnd(
        context: ParticipantContext,
        data: MatchEndData,
        bigStats: MatchEndData.BigStats,
        result: PlayerMatchResult
    ): ParticipantContext {
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

        profile.stats.bowShotsTaken = bigStats.bowShotsTaken
        profile.stats.bowShotsHit = bigStats.bowShotsHit
        profile.stats.damageGiven = bigStats.damageGiven
        profile.stats.damageTaken = bigStats.damageTaken
        profile.stats.damageGivenBow = bigStats.damageGivenBow

        // Make sure playtime is correct since it's only calculated on Party Leave (which isn't fired on Match End)
        val isPlaying = profile.partyName != null
        val joinedPartyAt = profile.joinedPartyAt
        if (isPlaying && joinedPartyAt != null) profile.stats.gamePlaytime += (context.match.endedAt!! - joinedPartyAt)

        return context
    }
}