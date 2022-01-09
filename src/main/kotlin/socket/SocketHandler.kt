package network.warzone.api.socket

import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import network.warzone.api.Config
import network.warzone.api.http.UnauthorizedException
import network.warzone.api.socket.server.ConnectedServers
import network.warzone.api.socket.server.ServerContext
import network.warzone.api.util.zlibDecompress

fun Application.initSocketHandler() {
    routing {
        webSocket("/minecraft") {
            val serverID = call.request.queryParameters["id"] ?: throw UnauthorizedException()
            val serverToken = call.request.queryParameters["token"] ?: throw UnauthorizedException()

            if (serverToken != Config.apiToken) throw UnauthorizedException()

            val server = ServerContext(serverID, this)
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

                    println("$eventName : $data")

                    val eventType = EventType.valueOf(eventName)

                    router.route(eventType, data)
                }
            } catch (err: Exception) {
                log.error(err)
            } finally {
                ConnectedServers -= server
                println("Server ${server.id} disconnected from socket server")
            }
        }
    }
}