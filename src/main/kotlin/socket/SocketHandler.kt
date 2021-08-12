package network.warzone.api.socket

import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import network.warzone.api.socket.listeners.LogListener
import network.warzone.api.socket.listeners.MatchListener
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
                    val eventName = json["e"]?.jsonPrimitive?.content ?: throw RuntimeException("Invalid event name")
                    val eventData = json["d"]?.jsonObject ?: throw RuntimeException("Invalid event data")
                    val eventType = SocketEvent.fromRawName(eventName) ?: throw RuntimeException("Invalid event type")

                    LogListener.handle(connection, eventType, eventData)
                    MatchListener.handle(connection, eventType, eventData)
                }
            } catch (err: Exception) {
                log.error(err)
            } finally {
                ConnectionStore.minecraftConnections -= connection
            }
        }
    }
}