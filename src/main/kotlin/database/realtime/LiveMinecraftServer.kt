package network.warzone.api.database.realtime

import io.ktor.http.cio.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import network.warzone.api.database.Redis
import network.warzone.api.socket.SocketEvent
import network.warzone.api.socket.format
import network.warzone.api.util.zlibCompress

data class LiveMinecraftServer(val id: String, val token: String, val session: DefaultWebSocketServerSession) {
    val currentMatch: LiveMatch?
        get() = Redis.get("match:$currentMatchId")

    var currentMatchId: String?
        get() = Redis.get("server:$id:current_match_id")
        set(matchId) = Redis.set("server:$id:current_match_id", matchId)

    suspend inline fun <reified T> call(type: SocketEvent, data: T) {
        val packet = Packet(type, data)
        val body = format.encodeToString(packet).zlibCompress()
        session.send(body)
    }
}

@Serializable
data class Packet<T>(@SerialName("e") val event: SocketEvent, @SerialName("d") val data: T)