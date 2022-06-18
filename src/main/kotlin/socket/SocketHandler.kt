package network.warzone.api.socket

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import network.warzone.api.Config
import network.warzone.api.http.UnauthorizedException
import network.warzone.api.socket.server.ConnectedServers
import network.warzone.api.socket.server.ServerContext
import network.warzone.api.util.zlibDecompress
import org.slf4j.Logger
import java.util.*

lateinit var logger: Logger

fun Application.initSocketHandler() {
    logger = log
    routing {
        webSocket("/minecraft") {
            val serverId = call.request.queryParameters["id"] ?: throw UnauthorizedException()
            val serverToken = call.request.queryParameters["token"] ?: throw UnauthorizedException()

            if (serverToken != Config.apiToken) throw UnauthorizedException()

            // Server ID already connected
            if (ConnectedServers.any { it.id == serverId }) {
                println("Server ID '$serverId' attempting to connect twice! Closing new connection...")
                return@webSocket this.close(
                    CloseReason(
                        CloseReason.Codes.VIOLATED_POLICY,
                        "Server with ID '$serverId' is already connected"
                    )
                )
            }

            val server = ServerContext(serverId, this)
            ConnectedServers += server
            println("Server '${server.id}' connected to socket server")

            val router = SocketRouter(server)

            try {
                for (frame in incoming) {
                    frame as? Frame.Binary ?: continue
                    val body = frame.readBytes().zlibDecompress()

                    val json = Json.parseToJsonElement(body).jsonObject
                    val eventName = json["e"]?.jsonPrimitive?.content ?: throw RuntimeException("Invalid event name")
                    val data = json["d"]?.jsonObject ?: throw RuntimeException("Invalid event data")

                    println("[$serverId:$eventName] $data")

                    val eventType = EventType.valueOf(eventName)

                    router.route(eventType, data)
                    server.lastAliveTime = Date().time
                }
            } catch (err: Exception) {
                err.printStackTrace()
            } finally {
                ConnectedServers -= server
                println("Server '${server.id}' disconnected from socket server")
            }
        }
    }
}