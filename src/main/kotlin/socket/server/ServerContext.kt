package network.warzone.api.socket.server

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import network.warzone.api.database.Redis
import network.warzone.api.database.models.Match
import network.warzone.api.socket.EventType
import network.warzone.api.util.zlibCompress

object ConnectedServers : HashSet<ServerContext>()

class ServerContext(val id: String, val session: DefaultWebSocketServerSession) {
    val match: Match?
        get() = Redis.get("match:$currentMatchId")

    var currentMatchId: String?
        get() = Redis.get("server:$id:current_match_id")
        set(matchId) = Redis.set("server:$id:current_match_id", matchId)

    var lastAliveTime: Long?
        get() = Redis.get("server:$id:last_alive_time")
        set(value) = Redis.set("server:$id:last_alive_time", value)

    suspend inline fun <reified T> call(type: EventType, data: T) {
        val packet = Packet(type, data)
        val body = Json.encodeToString(packet).zlibCompress()
        session.send(body)
    }
}

@Serializable
data class Packet<T>(@SerialName("e") val event: EventType, @SerialName("d") val data: T)