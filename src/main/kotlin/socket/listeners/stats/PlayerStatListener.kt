package network.warzone.api.socket.listeners.stats

import network.warzone.api.database.PlayerCache
import network.warzone.api.database.models.*
import network.warzone.api.socket.event.EventPriority
import network.warzone.api.socket.event.FireAt
import network.warzone.api.socket.event.Listener
import network.warzone.api.socket.listeners.death.PlayerDeathEvent
import network.warzone.api.socket.listeners.match.MatchEndEvent
import network.warzone.api.socket.listeners.objective.*

class PlayerStatListener : Listener() {
    override val handlers = mapOf(
        ::onDeath to PlayerDeathEvent::class,
        ::onKill to PlayerDeathEvent::class,
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

    @FireAt(EventPriority.EARLY)
    suspend fun onDeath(event: PlayerDeathEvent) {
        val victim = event.victim.getPlayer() ?: return

        // Increment victim's death count
        victim.stats.deaths++

        // If void death, increment victim's void death count
        if (event.data.cause == DamageCause.VOID) victim.stats.voidDeaths++

        event.victim.setPlayer(victim)
    }

    @FireAt(EventPriority.LATE)
    suspend fun onKill(event: PlayerDeathEvent) {
        val attacker = event.attacker?.getPlayer() ?: return
        val victim = event.victim.getPlayer() ?: return
        if (victim._id == attacker._id) return

        // Increment attacker's kill count
        attacker.stats.kills++

        // Modify attacker's weapon damage stats
        val weaponName = event.data.weapon ?: "NONE"
        val weaponDamageData = attacker.stats.weapons[weaponName] ?: WeaponDamageData(0)
        weaponDamageData.kills++
        attacker.stats.weapons[weaponName] = weaponDamageData

        // If void kill, increment attacker's void kill count
        if (event.data.cause == DamageCause.VOID)
            attacker.stats.voidKills++

        // If this is the first kill of the match, update first blood stats for player & match
        if (event.match.firstBlood == null) {
            victim.stats.firstBloodsSuffered++
            attacker.stats.firstBloods++
        }

        event.attacker.setPlayer(attacker)
        event.victim.setPlayer(victim)
    }

    @FireAt(EventPriority.EARLY)
    suspend fun onCoreLeak(event: CoreLeakEvent) {
        event.data.contributions.forEach {
            val participant = event.match.participants[it.playerId] ?: return
            val player = participant.getPlayer() ?: return
            player.stats.objectives.coreLeaks++
            participant.setPlayer(player)
        }
    }

    @FireAt(EventPriority.EARLY)
    suspend fun onControlPointCapture(event: ControlPointCaptureEvent) {
        event.data.playerIds.forEach {
            val participant = event.match.participants[it] ?: return
            val player = participant.getPlayer() ?: return
            player.stats.objectives.controlPointCaptures++
            participant.setPlayer(player)
        }
    }

    @FireAt(EventPriority.EARLY)
    suspend fun onDestroyableDestroy(event: DestroyableDestroyEvent) {
        event.data.contributions.forEach {
            val participant = event.match.participants[it.playerId] ?: return
            val player = participant.getPlayer() ?: return
            player.stats.objectives.destroyableDestroys++
            player.stats.objectives.destroyableBlockDestroys += it.blockCount
            participant.setPlayer(player)
        }
    }

    @FireAt(EventPriority.EARLY)
    suspend fun onFlagPlace(event: FlagPlaceEvent) {
        val participant = event.participant
        val player = participant.getPlayer() ?: return
        player.stats.objectives.flagCaptures++
        player.stats.objectives.totalFlagHoldTime += event.data.heldTime
        participant.setPlayer(player)
    }

    @FireAt(EventPriority.EARLY)
    suspend fun onFlagPickup(event: FlagPickupEvent) {
        val participant = event.participant
        val player = participant.getPlayer() ?: return
        player.stats.objectives.flagPickups++
        participant.setPlayer(player)
    }

    @FireAt(EventPriority.EARLY)
    suspend fun onFlagDrop(event: FlagDropEvent) {
        val participant = event.participant
        val player = participant.getPlayer() ?: return
        player.stats.objectives.flagDrops++
        player.stats.objectives.totalFlagHoldTime += event.data.heldTime
        participant.setPlayer(player)
    }

    @FireAt(EventPriority.EARLY)
    suspend fun onFlagDefend(event: FlagDefendEvent) {
        val participant = event.participant
        val player = participant.getPlayer() ?: return
        player.stats.objectives.flagDefends++
        participant.setPlayer(player)
    }

    @FireAt(EventPriority.EARLY)
    suspend fun onWoolPlace(event: WoolPlaceEvent) {
        val participant = event.participant
        val player = participant.getPlayer() ?: return
        player.stats.objectives.woolCaptures++
        participant.setPlayer(player)
    }

    @FireAt(EventPriority.EARLY)
    suspend fun onWoolPickup(event: WoolPickupEvent) {
        val participant = event.participant
        val player = participant.getPlayer() ?: return
        player.stats.objectives.woolPickups++
        participant.setPlayer(player)
    }

    @FireAt(EventPriority.EARLY)
    suspend fun onWoolDrop(event: WoolDropEvent) {
        val participant = event.participant
        val player = participant.getPlayer() ?: return
        player.stats.objectives.woolDrops++
        participant.setPlayer(player)
    }

    @FireAt(EventPriority.EARLY)
    suspend fun onWoolDefend(event: WoolDefendEvent) {
        val participant = event.participant
        val player = participant.getPlayer() ?: return
        player.stats.objectives.woolDefends++
        participant.setPlayer(player)
    }

    @FireAt(EventPriority.EARLY)
    suspend fun onMatchEnd(event: MatchEndEvent) {
        println(event.data)
    }
}