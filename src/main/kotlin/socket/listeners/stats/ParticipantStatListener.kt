package network.warzone.api.socket.listeners.stats

import network.warzone.api.database.models.*
import network.warzone.api.socket.event.EventPriority
import network.warzone.api.socket.event.FireAt
import network.warzone.api.socket.event.Listener
import network.warzone.api.socket.listeners.death.PlayerDeathEvent
import network.warzone.api.socket.listeners.match.MatchEndEvent
import network.warzone.api.socket.listeners.objective.*

class ParticipantStatListener : Listener() {
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
        val victim = event.victim

        // Increment victim's death count
        victim.stats.deaths++

        // If void death, increment victim's void death count
        if (event.data.cause == DamageCause.VOID) victim.stats.voidDeaths++

        event.match.saveParticipants(event.victim)
    }

    @FireAt(EventPriority.LATE)
    suspend fun onKill(event: PlayerDeathEvent) {
        val attacker = event.attacker ?: return
        val victim = event.victim
        if (victim.id == attacker.id) return

        // Increment attacker's kill count
        attacker.stats.kills++

        // Modify attacker's weapon damage stats
        val weaponName = event.data.weapon ?: "NONE"
        val weaponDamageData = attacker.stats.weapons[weaponName] ?: WeaponDamageData(0)
        weaponDamageData.kills++
        attacker.stats.weapons[weaponName] = weaponDamageData

        // Modify attacker and victim duel objects
        val attackerVictimDuel = attacker.stats.duels[victim.id] ?: Duel()
        val victimAttackerDuel = victim.stats.duels[attacker.id] ?: Duel()
        attackerVictimDuel.kills++
        victimAttackerDuel.deaths++
        attacker.stats.duels[victim.id] = attackerVictimDuel
        victim.stats.duels[attacker.id] = victimAttackerDuel

        // If void kill, increment attacker's void kill count
        if (event.data.cause == DamageCause.VOID)
            attacker.stats.voidKills++

        // If this is the first kill of the match, update first blood stats for player & match
        if (event.match.firstBlood == null) {
            event.match.firstBlood = FirstBlood(attacker.simplePlayer, victim.simplePlayer, System.currentTimeMillis())
            // todo: update player objs
        }

        event.match.saveParticipants(attacker, victim)
    }

    @FireAt(EventPriority.EARLY)
    suspend fun onCoreLeak(event: CoreLeakEvent) {
        val contributors = event.data.contributions.map { event.match.participants[it.playerId]!! }.toTypedArray()
        contributors.forEach { it.stats.objectives.coreLeaks++ }
        event.match.saveParticipants(*contributors)
    }

    @FireAt(EventPriority.EARLY)
    suspend fun onControlPointCapture(event: ControlPointCaptureEvent) {
        val contributors = event.data.playerIds.map { event.match.participants[it]!! }.toTypedArray()
        contributors.forEach { it.stats.objectives.controlPointCaptures++ }
        event.match.saveParticipants(*contributors)
    }

    @FireAt(EventPriority.EARLY)
    suspend fun onDestroyableDestroy(event: DestroyableDestroyEvent) {
        val contributors = mutableListOf<Participant>()
        event.data.contributions.forEach {
            val contributor = event.match.participants[it.playerId]!!
            contributor.stats.objectives.destroyableDestroys++
            contributor.stats.objectives.destroyableBlockDestroys += it.blockCount
            contributors.add(contributor)
        }
        event.match.saveParticipants(*contributors.toTypedArray())
    }

    @FireAt(EventPriority.EARLY)
    suspend fun onFlagPlace(event: FlagPlaceEvent) {
        event.participant.stats.objectives.flagCaptures++
        event.participant.stats.objectives.totalFlagHoldTime += event.data.heldTime
        event.match.saveParticipants(event.participant)
    }

    @FireAt(EventPriority.EARLY)
    suspend fun onFlagPickup(event: FlagPickupEvent) {
        val participant = event.participant
        participant.stats.objectives.flagPickups++
        event.match.saveParticipants(participant)
    }

    @FireAt(EventPriority.EARLY)
    suspend fun onFlagDrop(event: FlagDropEvent) {
        val participant = event.participant
        participant.stats.objectives.flagDrops++
        participant.stats.objectives.totalFlagHoldTime += event.data.heldTime
        event.match.saveParticipants(participant)
    }

    @FireAt(EventPriority.EARLY)
    suspend fun onFlagDefend(event: FlagDefendEvent) {
        val participant = event.participant
        participant.stats.objectives.flagDefends++
        event.match.saveParticipants(participant)
    }

    @FireAt(EventPriority.EARLY)
    suspend fun onWoolPlace(event: WoolPlaceEvent) {
        val participant = event.participant
        participant.stats.objectives.woolCaptures++
        event.match.saveParticipants(participant)
    }

    @FireAt(EventPriority.EARLY)
    suspend fun onWoolPickup(event: WoolPickupEvent) {
        val participant = event.participant
        participant.stats.objectives.woolPickups++
        event.match.saveParticipants(participant)
    }

    @FireAt(EventPriority.EARLY)
    suspend fun onWoolDrop(event: WoolDropEvent) {
        val participant = event.participant
        participant.stats.objectives.woolDrops++
        event.match.saveParticipants(participant)
    }

    @FireAt(EventPriority.EARLY)
    suspend fun onWoolDefend(event: WoolDefendEvent) {
        val participant = event.participant
        participant.stats.objectives.woolDefends++
        event.match.saveParticipants(participant)
    }

    @FireAt(EventPriority.EARLY)
    suspend fun onMatchEnd(event: MatchEndEvent) {
        val participants = mutableListOf<Participant>()
        event.data.bigStats.forEach {
            val id = it.key
            val participant = event.match.participants[id] ?: return

            val stats = it.value

            val blocks = stats.blocks
            blocks?.blocksBroken?.forEach { blockInteraction ->
                val block = blockInteraction.key
                var count = participant.stats.blocksBroken[block] ?: 0
                count += blockInteraction.value
                participant.stats.blocksBroken[block] = count
            }
            blocks?.blocksPlaced?.forEach { blockInteraction ->
                val block = blockInteraction.key
                var count = participant.stats.blocksPlaced[block] ?: 0
                count += blockInteraction.value
                participant.stats.blocksPlaced[block] = count
            }

            participant.stats.messages.staff += stats.messages.staff
            participant.stats.messages.global += stats.messages.global
            participant.stats.messages.team += stats.messages.team
            participant.stats.bowShotsTaken += stats.bowShotsTaken
            participant.stats.bowShotsHit += stats.bowShotsHit
            participant.stats.damageGiven += stats.damageGiven
            participant.stats.damageTaken += stats.damageTaken
            participant.stats.damageGivenBow += stats.damageGivenBow

            participants.add(participant)
        }
        event.match.saveParticipants(*participants.toTypedArray())
    }
}