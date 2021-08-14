package network.warzone.api.socket

import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import network.warzone.api.database.realtime.LiveMinecraftServer
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

            val server = LiveMinecraftServer(serverID, serverToken, this)
            ConnectionStore.minecraftServers += server

            try {
                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    val body = Json.decodeFromString<ByteArray>(frame.readText()).zlibDecompress()

                    val json = Json.parseToJsonElement(body).jsonObject
                    val eventName = json["e"]?.jsonPrimitive?.content ?: throw RuntimeException("Invalid event name")
                    val eventData = json["d"]?.jsonObject ?: throw RuntimeException("Invalid event data")
                    val eventType = SocketEvent.valueOf(eventName)

                    LogListener.handle(server, eventType, eventData)
                    MatchListener.handle(server, eventType, eventData)
                }
            } catch (err: Exception) {
                log.error(err)
            } finally {
                ConnectionStore.minecraftServers -= server
            }
        }
    }
}