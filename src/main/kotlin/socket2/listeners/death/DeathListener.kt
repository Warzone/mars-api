package network.warzone.api.socket2.listeners.death

import kotlinx.serialization.Serializable
import network.warzone.api.socket.listeners.DamageCause
import network.warzone.api.socket2.event.Listener
import network.warzone.api.socket2.listeners.match.LiveMatch
import network.warzone.api.socket2.listeners.match.MatchEvent

class DeathListener : Listener() {
    override val handlers = mapOf(
        ::onDeath to PlayerDeathEvent::class
    )

    suspend fun onDeath(event: PlayerDeathEvent) {

    }
}

class PlayerDeathEvent(match: LiveMatch, val data: PlayerDeathData) : MatchEvent(match) {
    @Serializable
    data class PlayerDeathData(
        val victimId: String,
        val victimName: String,
        val attackerId: String? = null,
        val attackerName: String? = null,
        val weapon: String? = null,
        val entity: String? = null,
        val distance: Int? = null,
        val key: String,
        val cause: DamageCause
    )
}