package network.warzone.api.socket

import io.ktor.websocket.*
import kotlinx.serialization.Serializable

interface SocketEvent {
    suspend fun fire(socket: DefaultWebSocketServerSession)
}

@Serializable
enum class SocketEventType(val rawName: String) {
    IDENTIFY("IDENTIFY");

    companion object {
        fun fromRawName(value: String): SocketEventType? = values().find { it.rawName  == value }
    }
}