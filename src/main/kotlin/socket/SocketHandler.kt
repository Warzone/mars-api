package network.warzone.api.socket

import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import network.warzone.api.socket.connection.ConnectionStore
import network.warzone.api.socket.connection.MinecraftConnection
import network.warzone.api.socket.connection.MinecraftServerInfo
import network.warzone.api.socket.event.inbound.match.MatchLoadEvent
import network.warzone.api.socket.event.inbound.match.MatchStartEvent
import network.warzone.api.util.zlibDecompress

fun Application.initSocketHandler() {
    routing {
        webSocket("/minecraft") {
            val serverID = call.request.queryParameters["id"]
            val serverToken = call.request.queryParameters["token"]

            // todo: don't hardcode credentials + support multiple servers
            if (!(serverID == "main" && serverToken == "secret")) throw RuntimeException("Invalid server ID or server token")

            val connection = MinecraftConnection(
                MinecraftServerInfo(serverID, serverToken),
                this
            )

            ConnectionStore.minecraftConnections += connection

            try {
                for (frame in incoming) {
                    frame as? Frame.Binary ?: continue

                    val body = frame.data.zlibDecompress()

                    val json = Json.parseToJsonElement(body).jsonObject
                    val eventName = json["e"]?.jsonPrimitive!!.content
                    val eventData = json["d"]?.jsonObject!!
                    val eventType = SocketEventType.fromRawName(eventName)

                    log.info("Minecraft socket event! | Server: ${connection.serverInfo.id} | Type: $eventName | Data: $eventData")

                    when (eventType) {
                        SocketEventType.MATCH_LOAD -> MatchLoadEvent(Json.decodeFromJsonElement(eventData)).fire(connection)
                        SocketEventType.MATCH_START -> MatchStartEvent().fire(connection)
                        else -> println("Fallback - received event $eventType")
                    }

                }
            } catch (err: Exception) {
                println(err)
            } finally {
                ConnectionStore.minecraftConnections -= connection
            }

        }
    }
}