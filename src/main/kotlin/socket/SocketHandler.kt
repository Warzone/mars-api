package network.warzone.api.socket

import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import network.warzone.api.socket.event.inbound.IdentifyEvent
import network.warzone.api.util.zlibDecompress
import java.util.Collections
import kotlin.collections.LinkedHashSet

fun Application.initSocketHandler() {
    routing {
        val connections = Collections.synchronizedSet<DefaultWebSocketServerSession?>(LinkedHashSet())

        webSocket("/minecraft") {
            connections += this

            try {
                for (frame in incoming) {
                    frame as? Frame.Binary ?: continue

                    val bodyString = frame.data.zlibDecompress()

                    val json = Json.parseToJsonElement(bodyString).jsonObject
                    val eventName = json["e"]?.jsonPrimitive!!.content
                    val eventData = json["d"]?.jsonObject!!
                    val eventType = SocketEventType.fromRawName(eventName)

                    when (eventType) {
                        SocketEventType.IDENTIFY -> IdentifyEvent(Json.decodeFromJsonElement(eventData)).fire(this)
                        null -> Unit
                    }

                }
            } catch (err: Exception) {
                println(err)
            } finally {
                connections -= this
            }

        }
    }
}