package network.warzone.api.socket.listeners.stats

import network.warzone.api.database.PlayerCache
import network.warzone.api.database.models.DamageCause
import network.warzone.api.database.models.Player
import network.warzone.api.socket.event.EventPriority
import network.warzone.api.socket.event.FireAt
import network.warzone.api.socket.event.Listener
import network.warzone.api.socket.listeners.death.PlayerDeathEvent
import network.warzone.api.socket.listeners.match.MatchEndEvent
import network.warzone.api.socket.listeners.objective.*
import kotlin.math.max
import kotlin.math.min

class GamemodeStatListener : Listener() {
    override val handlers = mapOf(
        ::onDeath to PlayerDeathEvent::class,
        ::onKill to PlayerDeathEvent::class,
        ::onKillstreak to KillstreakEvent::class,
        ::onCoreLeak to CoreLeakEvent::class,
        ::onControlPointCapture to ControlPointCaptureEvent::class,
        ::onDestroyableDestroy to DestroyableDestroyEvent::class,
        ::onFlagPlace to FlagPlaceEvent::class,
        ::onFlagPickup to FlagPickupEvent::class,
        ::onFlagDrop to FlagDropEvent::class,
        ::onFlagDefend to FlagDefendEvent::class,
        ::onWoolPlace to WoolPlaceEvent::class,
        ::onWoolPickup to WoolPickupEvent::class,
        ::onWoolDrop to WoolDropEvent::class,
        ::onWoolDefend to WoolDefendEvent::class,
        ::onMatchEnd to MatchEndEvent::class
    )

    @FireAt(EventPriority.LATEST)
    suspend fun onDeath(event: PlayerDeathEvent) {
        val victim = event.victim.getPlayer() ?: return
        victim.setGamemodeStats(event.match.level.gamemodes) {
            // Increment victim's death count
            it.deaths++

            // If void death, increment victim's void death count
            if (event.data.cause == DamageCause.VOID) it.voidDeaths++

            // Increment first bloods suffered stat
            if (event.match.firstBlood == null) it.firstBloodsSuffered++

            // Modify victim's weapon damage stats
            val weaponName = event.data.weapon ?: "NONE"
            var weaponDeaths = it.weaponDeaths[weaponName] ?: 0
            it.weaponDeaths[weaponName] = ++weaponDeaths

            return@setGamemodeStats it
        }
        event.victim.setPlayer(victim)
    }

    @FireAt(EventPriority.LATEST)
    suspend fun onKill(event: PlayerDeathEvent) {
        val attacker = event.attacker?.getPlayer() ?: return
        if (event.victim.id == attacker._id) return

        attacker.setGamemodeStats(event.match.level.gamemodes) {
            // Increment attacker's kill count
            it.kills++

            // Modify attacker's weapon damage stats
            val weaponName = event.data.weapon ?: "NONE"
            var weaponKills = it.weaponKills[weaponName] ?: 0
            it.weaponKills[weaponName] = ++weaponKills

            // If void kill, increment attacker's void kill count
            if (event.data.cause == DamageCause.VOID) it.voidKills++

            // Increment first blood count
            if (event.match.firstBlood == null) it.firstBloods++

            return@setGamemodeStats it
        }

        event.attacker.setPlayer(attacker)
    }

    @FireAt(EventPriority.LATEST)
    suspend fun onKillstreak(event: KillstreakEvent) {
        val player: Player = PlayerCache.get(event.data.player.name) ?: return
        player.setGamemodeStats(event.match.level.gamemodes) {
            var amount = it.killstreaks[event.data.amount] ?: 0
            it.killstreaks[event.data.amount] = ++amount
            return@setGamemodeStats it
        }
        PlayerCache.set(player.name, player)
    }

    @FireAt(EventPriority.LATEST)
    suspend fun onCoreLeak(event: CoreLeakEvent) {
        event.data.contributions.forEach {
            val participant = event.match.participants[it.playerId] ?: return
            val player = participant.getPlayer() ?: return
            player.setGamemodeStats(event.match.level.gamemodes) {
                it.objectives.coreLeaks++
                return@setGamemodeStats it
            }
            participant.setPlayer(player)
        }
    }

    @FireAt(EventPriority.LATEST)
    suspend fun onControlPointCapture(event: ControlPointCaptureEvent) {
        event.data.playerIds.forEach {
            val participant = event.match.participants[it] ?: return
            val player = participant.getPlayer() ?: return
            player.setGamemodeStats(event.match.level.gamemodes) {
                it.objectives.controlPointCaptures++
                return@setGamemodeStats it
            }
            participant.setPlayer(player)
        }
    }

    @FireAt(EventPriority.LATEST)
    suspend fun onDestroyableDestroy(event: DestroyableDestroyEvent) {
        event.data.contributions.forEach { contribution ->
            val participant = event.match.participants[contribution.playerId] ?: return
            val player = participant.getPlayer() ?: return
            player.setGamemodeStats(event.match.level.gamemodes) {
                it.objectives.destroyableDestroys++
                it.objectives.destroyableBlockDestroys += contribution.blockCount
                return@setGamemodeStats it
            }
            participant.setPlayer(player)
        }
    }

    @FireAt(EventPriority.LATEST)
    suspend fun onFlagPlace(event: FlagPlaceEvent) {
        val participant = event.participant
        val player = participant.getPlayer() ?: return
        player.setGamemodeStats(event.match.level.gamemodes) {
            it.objectives.flagCaptures++
            it.objectives.totalFlagHoldTime += event.data.heldTime
            return@setGamemodeStats it
        }
        participant.setPlayer(player)
    }

    @FireAt(EventPriority.LATEST)
    suspend fun onFlagPickup(event: FlagPickupEvent) {
        val participant = event.participant
        val player = participant.getPlayer() ?: return
        player.setGamemodeStats(event.match.level.gamemodes) {
            it.objectives.flagPickups++
            return@setGamemodeStats it
        }
        participant.setPlayer(player)
    }

    @FireAt(EventPriority.LATEST)
    suspend fun onFlagDrop(event: FlagDropEvent) {
        val participant = event.participant
        val player = participant.getPlayer() ?: return
        player.setGamemodeStats(event.match.level.gamemodes) {
            it.objectives.flagDrops++
            it.objectives.totalFlagHoldTime += event.data.heldTime
            return@setGamemodeStats it
        }
        participant.setPlayer(player)
    }

    @FireAt(EventPriority.LATEST)
    suspend fun onFlagDefend(event: FlagDefendEvent) {
        val participant = event.participant
        val player = participant.getPlayer() ?: return
        player.setGamemodeStats(event.match.level.gamemodes) {
            it.objectives.flagDefends++
            return@setGamemodeStats it
        }
        participant.setPlayer(player)
    }

    @FireAt(EventPriority.LATEST)
    suspend fun onWoolPlace(event: WoolPlaceEvent) {
        val participant = event.participant
        val player = participant.getPlayer() ?: return
        player.setGamemodeStats(event.match.level.gamemodes) {
            it.objectives.woolCaptures++
            return@setGamemodeStats it
        }
        participant.setPlayer(player)
    }

    @FireAt(EventPriority.LATEST)
    suspend fun onWoolPickup(event: WoolPickupEvent) {
        val participant = event.participant
        val player = participant.getPlayer() ?: return
        player.setGamemodeStats(event.match.level.gamemodes) {
            it.objectives.woolPickups++
            return@setGamemodeStats it
        }
        participant.setPlayer(player)
    }

    @FireAt(EventPriority.LATEST)
    suspend fun onWoolDrop(event: WoolDropEvent) {
        val participant = event.participant
        val player = participant.getPlayer() ?: return
        player.setGamemodeStats(event.match.level.gamemodes) {
            it.objectives.woolDrops++
            return@setGamemodeStats it
        }
        participant.setPlayer(player)
    }

    @FireAt(EventPriority.LATEST)
    suspend fun onWoolDefend(event: WoolDefendEvent) {
        val participant = event.participant
        val player = participant.getPlayer() ?: return
        player.setGamemodeStats(event.match.level.gamemodes) {
            it.objectives.woolDefends++
            return@setGamemodeStats it
        }
        participant.setPlayer(player)
    }

    @FireAt(EventPriority.MONITOR)
    suspend fun onMatchEnd(event: MatchEndEvent) {
        val tie =
            event.data.winningParties.isEmpty() || event.data.winningParties.count() == event.match.parties.count()

        event.match.participants.values.forEach { participant ->
            val id = participant.id
            val player = participant.getPlayer() ?: return

            player.setGamemodeStats(event.match.level.gamemodes) { stats ->
                val bigStats = event.data.bigStats[id]

                val isPlaying = participant.partyName != null

                // Make sure playtime is correct since it's only calculated on Party Leave (which isn't fired on Match End)
                val joinedPartyAt = participant.joinedPartyAt
                if (isPlaying && joinedPartyAt != null) stats.gamePlaytime += (event.match.endedAt!! - joinedPartyAt)

                if (bigStats !== null) {
                    val blocks = bigStats.blocks
                    blocks?.blocksBroken?.forEach { blockInteraction ->
                        val block = blockInteraction.key
                        var count = stats.blocksBroken[block] ?: 0
                        count += blockInteraction.value
                        stats.blocksBroken[block] = count
                    }
                    blocks?.blocksPlaced?.forEach { blockInteraction ->
                        val block = blockInteraction.key
                        var count = stats.blocksPlaced[block] ?: 0
                        count += blockInteraction.value
                        stats.blocksPlaced[block] = count
                    }

                    stats.messages.staff += bigStats.messages.staff
                    stats.messages.global += bigStats.messages.global
                    stats.messages.team += bigStats.messages.team
                    stats.bowShotsTaken += bigStats.bowShotsTaken
                    stats.bowShotsHit += bigStats.bowShotsHit
                    stats.damageGiven += bigStats.damageGiven
                    stats.damageTaken += bigStats.damageTaken
                    stats.damageGivenBow += bigStats.damageGivenBow
                }

                if (tie && isPlaying) stats.ties++
                else if (!tie && event.data.winningParties.contains(participant.partyName)) stats.wins++
                else if (isPlaying && !event.data.winningParties.contains(participant.partyName)) stats.losses++

                // min ( 10% of match length | 1 minute )
                val minimumPlaytime = min(0.10 * event.match.length, 60000.0)

                // How much time between match start and player first joining match?
                val firstJoinedMatchAt = max(participant.firstJoinedMatchAt - event.match.startedAt!!, 0)
                val presentAtStart = firstJoinedMatchAt < minimumPlaytime

                if (participant.stats.gamePlaytime > minimumPlaytime) stats.matches++
                if (presentAtStart) stats.matchesPresentStart++
                if (participant.stats.timeAway < 20000 && isPlaying) stats.matchesPresentFull++
                if (isPlaying) stats.matchesPresentEnd++

                stats.gamePlaytime += participant.stats.gamePlaytime

                return@setGamemodeStats stats
            }

            PlayerCache.set(player.nameLower, player, true)
        }
    }
}