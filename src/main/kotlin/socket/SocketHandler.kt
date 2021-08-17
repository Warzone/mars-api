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
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import network.warzone.api.database.realtime.*
import network.warzone.api.socket.listeners.*
import network.warzone.api.socket.listeners.objective.*
import network.warzone.api.util.zlibDecompress
import java.util.*

val format = Json {
    serializersModule = SerializersModule {
        polymorphic(MatchEvent::class) {
            // match state events
            subclass(MatchLoadEvent::class)
            subclass(MatchStartEvent::class)

            // party events
            subclass(PartyMemberAddEvent::class)
            subclass(PartyMemberRemoveEvent::class)

            // player events
            subclass(PlayerDeathEvent::class)

            // objective events
            subclass(FlagPickupEvent::class)
            subclass(FlagCaptureEvent::class)
            subclass(FlagDropEvent::class)
            subclass(FlagDefendEvent::class)
            subclass(WoolPickupEvent::class)
            subclass(WoolCaptureEvent::class)
            subclass(WoolDropEvent::class)
            subclass(WoolDefendEvent::class)
            subclass(CoreLeakEvent::class)
            subclass(CoreDamageEvent::class)
            subclass(DestroyableDestroyEvent::class)
            subclass(DestroyableDamageEvent::class)
            subclass(ControlPointCaptureEvent::class)
        }
    }
}

fun Application.initSocketHandler() {
    routing {
        webSocket("/minecraft") {
            val serverID = call.request.queryParameters["id"]
            val serverToken = call.request.queryParameters["token"]

            // todo: don't hardcode credentials + support multiple servers
            if (!(serverID == "main" && serverToken == "secret")) throw RuntimeException("Invalid server ID or server token")

            val server = LiveMinecraftServer(serverID, serverToken, this)
            ConnectionStore.minecraftServers += server
            println("Server ${server.id} connected to socket server")

            try {
                for (frame in incoming) {
                    frame as? Frame.Binary ?: continue
                    val decompressed = frame.readBytes().zlibDecompress()
                    val body = Json.decodeFromString<ByteArray>(decompressed).zlibDecompress()

                    val json = Json.parseToJsonElement(body).jsonObject
                    val eventName = json["e"]?.jsonPrimitive?.content ?: throw RuntimeException("Invalid event name")
                    val eventData = json["d"]?.jsonObject ?: throw RuntimeException("Invalid event data")
                    val eventType = SocketEvent.valueOf(eventName)

                    LogListener.handle(server, eventType, eventData)
                    MatchListener.handle(server, eventType, eventData)
                    PlayerListener.handle(server, eventType, eventData)
                    PartyListener.handle(server, eventType, eventData)

                    ControlPointListener.handle(server, eventType, eventData)
                    CoreListener.handle(server, eventType, eventData)
                    DestroyableListener.handle(server, eventType, eventData)
                    FlagListener.handle(server, eventType, eventData)
                    WoolListener.handle(server, eventType, eventData)
                }
            } catch (err: Exception) {
                log.error(err)
            } finally {
                ConnectionStore.minecraftServers -= server
                println("Server ${server.id} disconnected from socket server")
            }
        }
    }
}