package network.warzone.api.socket.listeners.objective

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import network.warzone.api.database.realtime.LiveMinecraftServer
import network.warzone.api.database.realtime.MatchEvent
import network.warzone.api.socket.SocketEvent
import network.warzone.api.socket.SocketListener
import network.warzone.api.socket.format
import network.warzone.api.socket.listeners.GoalContribution

object DestroyableListener : SocketListener() {
    override suspend fun handle(server: LiveMinecraftServer, event: SocketEvent, json: JsonObject) {
        when (event) {
            SocketEvent.DESTROYABLE_DAMAGE -> onDamage(server, format.decodeFromJsonElement(json))
            SocketEvent.DESTROYABLE_DESTROY -> onDestroy(server, format.decodeFromJsonElement(json))
            else -> Unit
        }
    }

    private fun onDamage(server: LiveMinecraftServer, data: DestroyableDamageData) {
        val current = server.currentMatch ?: return println("No match")
        current.events.add(DestroyableDamageEvent(data))
        current.save()
    }

    private fun onDestroy(server: LiveMinecraftServer, data: DestroyableDestroyData) {
        val current = server.currentMatch ?: return println("No match")
        current.events.add(DestroyableDestroyEvent(data))
        current.save()
    }
}

@Serializable
data class DestroyableDestroyData(val destroyableId: String, val contributions: Set<GoalContribution>)

@Serializable
@SerialName("DESTROYABLE_DESTROY")
data class DestroyableDestroyEvent(val data: DestroyableDestroyData) : MatchEvent(SocketEvent.DESTROYABLE_DESTROY, System.currentTimeMillis())

@Serializable
data class DestroyableDamageData(val destroyableId: String, val playerId: String)

@Serializable
@SerialName("DESTROYABLE_DAMAGE")
data class DestroyableDamageEvent(val data: DestroyableDamageData) : MatchEvent(SocketEvent.DESTROYABLE_DAMAGE, System.currentTimeMillis())