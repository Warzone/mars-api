package network.warzone.api.socket.player

import network.warzone.api.database.models.DamageCause
import network.warzone.api.socket.match.MatchEndData
import network.warzone.api.socket.participant.PlayerMatchResult
import network.warzone.api.socket.participant.PlayerMatchResult.*
import kotlin.math.max
import kotlin.math.min

class PlayerGamemodeStatListener : PlayerListener<PlayerContext>() {
    override suspend fun onDeath(
        context: PlayerContext,
        data: PlayerDeathData,
        firstBlood: Boolean
    ): PlayerContext {
        return context.modifyGamemodeStats { stats ->
            stats.deaths++

            if (data.cause == DamageCause.VOID) stats.voidDeaths++

            if (firstBlood) stats.firstBloodsSuffered++

            if (data.isMurder) {
                val weaponName = data.weapon ?: "NONE"
                val weaponDeaths = stats.weaponDeaths[weaponName] ?: 0
                stats.weaponDeaths[weaponName] = weaponDeaths + 1
            }

            return@modifyGamemodeStats stats
        }
    }

    override suspend fun onKill(
        context: PlayerContext,
        data: PlayerDeathData,
        firstBlood: Boolean
    ): PlayerContext {
        return context.modifyGamemodeStats { stats ->
            stats.kills++

            if (firstBlood) stats.firstBloods++
            if (data.cause == DamageCause.VOID) stats.voidKills++

            val weaponName = data.weapon ?: "NONE"
            val weaponKills = stats.weaponKills[weaponName] ?: 0
            stats.weaponKills[weaponName] = weaponKills + 1

            return@modifyGamemodeStats stats
        }
    }

    override suspend fun onKillstreak(context: PlayerContext, amount: Int): PlayerContext {
        return context.modifyGamemodeStats { stats ->
            val prevAmount = stats.killstreaks[amount] ?: 0
            stats.killstreaks[amount] = prevAmount + 1
            return@modifyGamemodeStats stats
        }
    }

    override suspend fun onCoreLeak(
        context: PlayerContext,
        percentage: Float,
        blockCount: Int
    ): PlayerContext {
        return context.modifyGamemodeStats { stats ->
            stats.objectives.coreLeaks++
            stats.objectives.coreBlockDestroys += blockCount
            return@modifyGamemodeStats stats
        }
    }

    override suspend fun onControlPointCapture(context: PlayerContext, contributors: Int): PlayerContext {
        return context.modifyGamemodeStats { stats ->
            stats.objectives.controlPointCaptures++
            return@modifyGamemodeStats stats
        }
    }

    override suspend fun onDestroyableDestroy(
        context: PlayerContext,
        percentage: Float,
        blockCount: Int
    ): PlayerContext {
        return context.modifyGamemodeStats { stats ->
            stats.objectives.destroyableDestroys++
            stats.objectives.destroyableBlockDestroys += blockCount
            return@modifyGamemodeStats stats
        }
    }

    override suspend fun onFlagPlace(context: PlayerContext, heldTime: Long): PlayerContext {
        return context.modifyGamemodeStats { stats ->
            stats.objectives.flagCaptures++
            stats.objectives.totalFlagHoldTime += heldTime
            return@modifyGamemodeStats stats
        }
    }

    override suspend fun onFlagPickup(context: PlayerContext): PlayerContext {
        return context.modifyGamemodeStats { stats ->
            stats.objectives.flagPickups++
            return@modifyGamemodeStats stats
        }
    }

    override suspend fun onFlagDrop(context: PlayerContext, heldTime: Long): PlayerContext {
        return context.modifyGamemodeStats { stats ->
            stats.objectives.flagDrops++
            stats.objectives.totalFlagHoldTime += heldTime
            return@modifyGamemodeStats stats
        }
    }

    override suspend fun onFlagDefend(context: PlayerContext): PlayerContext {
        return context.modifyGamemodeStats { stats ->
            stats.objectives.flagDefends++
            return@modifyGamemodeStats stats
        }
    }

    override suspend fun onWoolPlace(context: PlayerContext): PlayerContext {
        return context.modifyGamemodeStats { stats ->
            stats.objectives.woolCaptures++
            return@modifyGamemodeStats stats
        }
    }

    override suspend fun onWoolPickup(context: PlayerContext): PlayerContext {
        return context.modifyGamemodeStats { stats ->
            stats.objectives.woolPickups++
            return@modifyGamemodeStats stats
        }
    }

    override suspend fun onWoolDrop(context: PlayerContext): PlayerContext {
        return context.modifyGamemodeStats { stats ->
            stats.objectives.woolDrops++
            return@modifyGamemodeStats stats
        }
    }

    override suspend fun onWoolDefend(context: PlayerContext): PlayerContext {
        return context.modifyGamemodeStats { stats ->
            stats.objectives.woolDefends++
            return@modifyGamemodeStats stats
        }
    }

    override suspend fun onMatchEnd(
        context: PlayerContext,
        data: MatchEndData,
        bigStats: MatchEndData.BigStats,
        result: PlayerMatchResult
    ): PlayerContext {
        return context.modifyGamemodeStats { stats ->
            val blocks = bigStats.blocks
            blocks.blocksBroken.forEach { interaction ->
                val block = interaction.key
                stats.blocksBroken[block] = interaction.value
            }
            blocks.blocksPlaced.forEach { interaction ->
                val block = interaction.key
                stats.blocksPlaced[block] = interaction.value
            }

            stats.messages.global += bigStats.messages.global
            stats.messages.team += bigStats.messages.team
            stats.messages.staff += bigStats.messages.staff
            stats.bowShotsTaken += bigStats.bowShotsTaken
            stats.bowShotsHit += bigStats.bowShotsHit
            stats.damageGiven += bigStats.damageGiven
            stats.damageTaken += bigStats.damageTaken
            stats.damageGivenBow += bigStats.damageGivenBow

            val (participant) = context.getParticipant()

            // min ( 10% of match length | 1 minute )
            val minimumPlaytime = min(0.10 * context.match.length, 60000.0)
            val isPlaying = participant.partyName != null

            if (participant.stats.gamePlaytime > minimumPlaytime) {
                when (result) {
                    TIE -> stats.ties++
                    WIN -> stats.wins++
                    LOSE -> stats.losses++
                    else -> Unit
                }
            } else context.sendMessage(
                "&Your stats were not affected by the outcome of this match as you did not participate for long enough."
            )

            val timeElapsedBeforeJoining = max(participant.firstJoinedMatchAt - context.match.startedAt!!, 0)
            val wasPresentAtStart = timeElapsedBeforeJoining < minimumPlaytime

            if (participant.stats.gamePlaytime > minimumPlaytime) stats.matches++
            if (wasPresentAtStart) stats.matchesPresentStart++
            if (participant.stats.timeAway < 20000 && isPlaying) stats.matchesPresentFull++
            if (isPlaying) stats.matchesPresentEnd++

            stats.gamePlaytime += participant.stats.gamePlaytime

            return@modifyGamemodeStats stats
        }
    }
}