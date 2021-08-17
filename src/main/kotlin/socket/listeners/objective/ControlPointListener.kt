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

object ControlPointListener : SocketListener() {
    override suspend fun handle(server: LiveMinecraftServer, event: SocketEvent, json: JsonObject) {
        when (event) {
            SocketEvent.CONTROL_POINT_CAPTURE -> onCapture(server, format.decodeFromJsonElement(json))
            else -> Unit
        }
    }

    private fun onCapture(server: LiveMinecraftServer, data: ControlPointCaptureData) {
        val current = server.currentMatch ?: return println("No match")
        current.events.add(ControlPointCaptureEvent(data))
        current.save()
    }
}

@Serializable
data class ControlPointCaptureData(val pointId: String, val playerIds: List<String>, val partyName: String)

@Serializable
@SerialName("CONTROL_POINT_CAPTURE")
data class ControlPointCaptureEvent(val data: ControlPointCaptureData) : MatchEvent(SocketEvent.CONTROL_POINT_CAPTURE, System.currentTimeMillis())