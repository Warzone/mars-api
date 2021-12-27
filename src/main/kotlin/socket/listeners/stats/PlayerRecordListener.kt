package network.warzone.api.socket.listeners.stats

import kotlinx.serialization.Serializable
import network.warzone.api.database.models.Match
import network.warzone.api.socket.event.EventPriority
import network.warzone.api.socket.event.FireAt
import network.warzone.api.socket.event.Listener
import network.warzone.api.socket.listeners.match.MatchEvent

class PlayerRecordListener : Listener() {
    override val handlers = mapOf(
        ::onProjectileHit to ProjectileHitEvent::class
    )

    @FireAt(EventPriority.LATEST)
    suspend fun onProjectileHit(event: ProjectileHitEvent) {

    }
}

class ProjectileHitEvent(match: Match, val data: ProjectileHitData) : MatchEvent(match) {
    @Serializable
    data class ProjectileHitData(val playerId: String, val type: String, val distance: Int)
}