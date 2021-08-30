package network.warzone.api.socket2.listeners.player

import kotlinx.serialization.Serializable
import network.warzone.api.socket.listeners.DamageCause
import network.warzone.api.socket2.event.EventPriority
import network.warzone.api.socket2.event.FireAt
import network.warzone.api.socket2.event.Listener
import network.warzone.api.socket2.listeners.death.PlayerDeathEvent
import network.warzone.api.socket2.listeners.match.LiveMatch
import network.warzone.api.socket2.listeners.match.MatchEvent

class PlayerStatListener : Listener() {
    override val handlers = mapOf(
        ::onDeath to PlayerDeathEvent::class
    )

    @FireAt(EventPriority.EARLY)
    suspend fun onDeath(event: PlayerDeathEvent) {
        val victim = event.match.participants[event.data.victimId] ?: return
        victim.deaths++
        victim.save()

        val attacker = event.match.participants[event.data.attackerId]
        if (attacker != null && victim != attacker) {
            attacker.kills++
            attacker.save()
        }
    }
}