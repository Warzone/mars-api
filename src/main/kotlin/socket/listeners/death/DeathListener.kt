package network.warzone.api.socket.listeners.death

import kotlinx.serialization.Serializable
import network.warzone.api.database.Database
import network.warzone.api.database.models.DamageCause
import network.warzone.api.database.models.Death
import network.warzone.api.database.models.SimplePlayer
import network.warzone.api.socket.event.EventPriority
import network.warzone.api.socket.event.FireAt
import network.warzone.api.socket.event.Listener
import network.warzone.api.socket.listeners.match.LiveMatch
import network.warzone.api.socket.listeners.match.MatchEvent
import java.util.*

class DeathListener : Listener() {
    override val handlers = mapOf(
        ::onDeath to PlayerDeathEvent::class
    )

    @FireAt(EventPriority.EARLY)
    suspend fun onDeath(event: PlayerDeathEvent) {
        val death = Death(
            _id = UUID.randomUUID().toString(),
            victim = event.victim.simplePlayer,
            attacker = event.attacker?.simplePlayer,
            weapon = event.data.weapon,
            entity = event.data.entity,
            distance = event.data.distance,
            key = event.data.key,
            cause = event.data.cause
        )
        Database.deaths.insertOne(death)
    }
}

class PlayerDeathEvent(match: LiveMatch, val data: PlayerDeathData) : MatchEvent(match) {
    val victim = match.participants[data.victimId]!!
    val attacker = match.participants[data.attackerId]

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