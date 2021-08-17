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

object FlagListener : SocketListener() {
    override suspend fun handle(server: LiveMinecraftServer, event: SocketEvent, json: JsonObject) {
        when (event) {
            SocketEvent.FLAG_DEFEND -> onDefend(server, format.decodeFromJsonElement(json))
            SocketEvent.FLAG_PICKUP -> onPickup(server, format.decodeFromJsonElement(json))
            SocketEvent.FLAG_DROP -> onDrop(server, format.decodeFromJsonElement(json))
            SocketEvent.FLAG_CAPTURE -> onCapture(server, format.decodeFromJsonElement(json))
            else -> Unit
        }
    }

    private fun onDefend(server: LiveMinecraftServer, data: FlagEventData) {
        val current = server.currentMatch ?: return println("No match")
        current.events.add(FlagDefendEvent(data))
        current.save()
    }

    private fun onPickup(server: LiveMinecraftServer, data: FlagEventData) {
        val current = server.currentMatch ?: return println("No match")
        current.events.add(FlagPickupEvent(data))
        current.save()
    }

    private fun onDrop(server: LiveMinecraftServer, data: FlagHeldData) {
        val current = server.currentMatch ?: return println("No match")
        current.events.add(FlagDropEvent(data))
        current.save()
    }

    private fun onCapture(server: LiveMinecraftServer, data: FlagHeldData) {
        val current = server.currentMatch ?: return println("No match")
        current.events.add(FlagCaptureEvent(data))
        current.save()
    }
}

@Serializable
data class FlagEventData(val flagId: String, val playerId: String)

@Serializable
data class FlagHeldData(val flagId: String, val playerId: String, val heldTime: Long)

@Serializable
@SerialName("FLAG_PICKUP")
data class FlagPickupEvent(val data: FlagEventData) : MatchEvent(SocketEvent.FLAG_PICKUP, System.currentTimeMillis())

@Serializable
@SerialName("FLAG_CAPTURE")
data class FlagCaptureEvent(val data: FlagHeldData) : MatchEvent(SocketEvent.FLAG_CAPTURE, System.currentTimeMillis())

@Serializable
@SerialName("FLAG_DROP")
data class FlagDropEvent(val data: FlagHeldData) : MatchEvent(SocketEvent.FLAG_DROP, System.currentTimeMillis())

@Serializable
@SerialName("FLAG_DEFEND")
data class FlagDefendEvent(val data: FlagEventData) : MatchEvent(SocketEvent.FLAG_DEFEND, System.currentTimeMillis())