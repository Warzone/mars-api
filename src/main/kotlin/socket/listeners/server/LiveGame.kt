package network.warzone.api.socket.listeners.server

import io.ktor.http.cio.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import network.warzone.api.database.Redis
import network.warzone.api.socket.event.EventType
import network.warzone.api.socket.listeners.match.LiveMatch
import network.warzone.api.util.zlibCompress

object ConnectedServers : HashSet<LiveGameServer>()

data class LiveGameServer(val id: String, val token: String, val session: DefaultWebSocketServerSession) {
    val currentMatch: LiveMatch?
        get() = Redis.get("match:$currentMatchId")

    var currentMatchId: String?
        get() = Redis.get("server:$id:current_match_id")
        set(matchId) = Redis.set("server:$id:current_match_id", matchId)

    suspend inline fun <reified T> call(type: EventType, data: T) {
        val packet = Packet(type, data)
        val body = Json.encodeToString(packet).zlibCompress()
        session.send(body)
    }
}

@Serializable
data class Packet<T>(@SerialName("e") val event: EventType, @SerialName("d") val data: T)