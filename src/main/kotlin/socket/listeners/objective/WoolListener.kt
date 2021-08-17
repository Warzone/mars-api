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

object WoolListener : SocketListener() {
    override suspend fun handle(server: LiveMinecraftServer, event: SocketEvent, json: JsonObject) {
        when (event) {
            SocketEvent.WOOL_DEFEND -> onDefend(server, format.decodeFromJsonElement(json))
            SocketEvent.WOOL_PICKUP -> onPickup(server, format.decodeFromJsonElement(json))
            SocketEvent.WOOL_DROP -> onDrop(server, format.decodeFromJsonElement(json))
            SocketEvent.WOOL_CAPTURE -> onCapture(server, format.decodeFromJsonElement(json))
            else -> Unit
        }
    }

    private fun onDefend(server: LiveMinecraftServer, data: WoolEventData) {
        val current = server.currentMatch ?: return println("No match")
        current.events.add(WoolDefendEvent(data))
        current.save()
    }

    private fun onPickup(server: LiveMinecraftServer, data: WoolEventData) {
        val current = server.currentMatch ?: return println("No match")
        current.events.add(WoolPickupEvent(data))
        current.save()
    }

    private fun onDrop(server: LiveMinecraftServer, data: WoolEventData) {
        val current = server.currentMatch ?: return println("No match")
        current.events.add(WoolDropEvent(data))
        current.save()
    }

    private fun onCapture(server: LiveMinecraftServer, data: WoolEventData) {
        val current = server.currentMatch ?: return println("No match")
        current.events.add(WoolCaptureEvent(data))
        current.save()
    }
}


@Serializable
data class WoolEventData(val woolId: String, val playerId: String)

@Serializable
@SerialName("WOOL_PICKUP")
data class WoolPickupEvent(val data: WoolEventData) : MatchEvent(SocketEvent.WOOL_PICKUP, System.currentTimeMillis())

@Serializable
@SerialName("WOOL_CAPTURE")
data class WoolCaptureEvent(val data: WoolEventData) : MatchEvent(SocketEvent.WOOL_CAPTURE, System.currentTimeMillis())

@Serializable
@SerialName("WOOL_DROP")
data class WoolDropEvent(val data: WoolEventData) : MatchEvent(SocketEvent.WOOL_DROP, System.currentTimeMillis())

@Serializable
@SerialName("WOOL_DEFEND")
data class WoolDefendEvent(val data: WoolEventData) : MatchEvent(SocketEvent.WOOL_DEFEND, System.currentTimeMillis())