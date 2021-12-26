package network.warzone.api.socket.listeners.stats

import kotlinx.serialization.Serializable
import network.warzone.api.database.PlayerCache
import network.warzone.api.database.models.*
import network.warzone.api.socket.event.EventPriority
import network.warzone.api.socket.event.EventType
import network.warzone.api.socket.event.FireAt
import network.warzone.api.socket.event.Listener
import network.warzone.api.socket.listeners.death.PlayerDeathEvent
import network.warzone.api.socket.listeners.match.MatchEndEvent
import network.warzone.api.socket.listeners.match.MatchEvent
import network.warzone.api.socket.listeners.objective.*
import kotlin.math.max
import kotlin.math.min

class PlayerStatListener : Listener() {
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

    @FireAt(EventPriority.EARLY)
    suspend fun onDeath(event: PlayerDeathEvent) {
        val victim = event.victim.getPlayer() ?: return

        // Increment victim's death count
        victim.stats.deaths++

        // If void death, increment victim's void death count
        if (event.data.cause == DamageCause.VOID) victim.stats.voidDeaths++

        if (event.match.firstBlood == null) victim.stats.firstBloodsSuffered++

        // Modify victim's weapon damage stats
        val weaponName = event.data.weapon ?: "NONE"
        var weaponDeaths = victim.stats.weaponDeaths[weaponName] ?: 0
        victim.stats.weaponDeaths[weaponName] = ++weaponDeaths

        event.victim.setPlayer(victim)
    }

    @FireAt(EventPriority.EARLY)
    suspend fun onKill(event: PlayerDeathEvent) {
        val attacker = event.attacker?.getPlayer() ?: return
        val victim = event.victim.getPlayer() ?: return
        if (victim._id == attacker._id) return

        // Increment attacker's kill count
        attacker.stats.kills++

        // Modify attacker's weapon damage stats
        val weaponName = event.data.weapon ?: "NONE"
        var weaponKills = attacker.stats.weaponKills[weaponName] ?: 0
        attacker.stats.weaponKills[weaponName] = ++weaponKills

        // If void kill, increment attacker's void kill count
        if (event.data.cause == DamageCause.VOID)
            attacker.stats.voidKills++

        // If this is the first kill of the match, update first blood stats for player & match
        if (event.match.firstBlood == null) {
            attacker.stats.firstBloods++
        }

        event.attacker.setPlayer(attacker)
        event.victim.setPlayer(victim)
    }

    @FireAt(EventPriority.EARLY)
    suspend fun onKillstreak(event: KillstreakEvent) {
        val player: Player = PlayerCache.get(event.data.player.name) ?: return
        var amount = player.stats.killstreaks[event.data.amount] ?: 0
        player.stats.killstreaks[event.data.amount] = ++amount
        PlayerCache.set(player.name, player)
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
        val tie = event.isTie()

        event.match.participants.values.forEach {
            val id = it.id
            val player = it.getPlayer() ?: return

            val bigStats = event.data.bigStats[id]

            val isPlaying = it.partyName != null

            // Make sure playtime is correct since it's only calculated on Party Leave (which isn't fired on Match End)
            val joinedPartyAt = it.joinedPartyAt
            if (isPlaying && joinedPartyAt != null) it.stats.gamePlaytime += (event.match.endedAt!! - joinedPartyAt)

            if (bigStats !== null) {
                val blocks = bigStats.blocks
                blocks?.blocksBroken?.forEach { blockInteraction ->
                    val block = blockInteraction.key
                    var count = player.stats.blocksBroken[block] ?: 0
                    count += blockInteraction.value
                    player.stats.blocksBroken[block] = count
                }
                blocks?.blocksPlaced?.forEach { blockInteraction ->
                    val block = blockInteraction.key
                    var count = player.stats.blocksPlaced[block] ?: 0
                    count += blockInteraction.value
                    player.stats.blocksPlaced[block] = count
                }

                player.stats.messages.staff += bigStats.messages.staff
                player.stats.messages.global += bigStats.messages.global
                player.stats.messages.team += bigStats.messages.team
                player.stats.bowShotsTaken += bigStats.bowShotsTaken
                player.stats.bowShotsHit += bigStats.bowShotsHit
                player.stats.damageGiven += bigStats.damageGiven
                player.stats.damageTaken += bigStats.damageTaken
                player.stats.damageGivenBow += bigStats.damageGivenBow
            }

            // min ( 10% of match length | 1 minute )
            val minimumPlaytime = min(0.10 * event.match.length, 60000.0)

            val playerResult = it.resultInMatch(event)
            if (it.stats.gamePlaytime > minimumPlaytime) {
                if (tie) player.stats.ties++
                else if (playerResult == PlayerMatchResult.WIN) player.stats.wins++
                else if (playerResult == PlayerMatchResult.LOSE) player.stats.losses++
            } else {
                event.server.call(
                    EventType.MESSAGE,
                    MessageData(
                        "&cThe outcome of this match has not affected your stats as you did not participate for long enough.",
                        null,
                        listOf(it.id)
                    )
                )
            }

            // How much time between match start and player first joining match?
            val firstJoinedMatchAt = max(it.firstJoinedMatchAt - event.match.startedAt!!, 0)
            val presentAtStart = firstJoinedMatchAt < minimumPlaytime

            if (it.stats.gamePlaytime > minimumPlaytime) player.stats.matches++
            if (presentAtStart) player.stats.matchesPresentStart++
            if (it.stats.timeAway < 20000 && isPlaying) player.stats.matchesPresentFull++
            if (isPlaying) player.stats.matchesPresentEnd++

            player.stats.gamePlaytime += it.stats.gamePlaytime

            PlayerCache.set(player.nameLower, player, true)
        }
    }
}

class KillstreakEvent(match: Match, val data: KillstreakData) : MatchEvent(match) {
    val participant = match.participants[data.player.id]
        ?: throw RuntimeException("Player ${data.player.id} is not a participant")

    @Serializable
    data class KillstreakData(val amount: Int, val player: SimplePlayer, val ended: Boolean)
}
