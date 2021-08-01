package network.warzone.api.socket.event.inbound

import io.ktor.websocket.*
import kotlinx.serialization.Serializable
import network.warzone.api.socket.SocketEvent

@Serializable
data class IdentifyEventData(val serverId: String, val serverToken: String)

class IdentifyEvent(private val data: IdentifyEventData) : SocketEvent {
    override suspend fun fire(socket: DefaultWebSocketServerSession) {
        println("Identify event was called: ${data.serverId} - ${data.serverToken}")
    }
}