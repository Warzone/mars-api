package network.warzone.api.socket2

import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import network.warzone.api.socket2.event.*
import network.warzone.api.socket2.listeners.chat.ChatListener
import network.warzone.api.socket2.listeners.chat.PlayerChatEvent
import network.warzone.api.socket2.listeners.match.MatchEndEvent
import network.warzone.api.socket2.listeners.match.MatchListener
import network.warzone.api.socket2.listeners.match.MatchLoadEvent
import network.warzone.api.socket2.listeners.match.MatchStartEvent
import network.warzone.api.socket2.listeners.party.PartyJoinEvent
import network.warzone.api.socket2.listeners.party.PartyLeaveEvent
import network.warzone.api.socket2.listeners.party.PartyListener
import network.warzone.api.socket2.listeners.player.PlayerDeathEvent
import network.warzone.api.socket2.listeners.player.PlayerStatListener
import network.warzone.api.socket2.listeners.server.ConnectedServers
import network.warzone.api.socket2.listeners.server.LiveGameServer
import network.warzone.api.util.zlibDecompress
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.jvm.javaMethod

//val format = Json {
//    serializersModule = SerializersModule {
//        polymorphic(MatchEvent::class) {
//            // match state events
////            subclass(MatchLoadEvent::class)
//            subclass(MatchStartEvent::class)
//
//            // party events
//            subclass(PartyMemberAddEvent::class)
//            subclass(PartyMemberRemoveEvent::class)
//
//            // player events
//            subclass(PlayerDeathEvent::class)
//
//            // objective events
//            subclass(FlagPickupEvent::class)
//            subclass(FlagCaptureEvent::class)
//            subclass(FlagDropEvent::class)
//            subclass(FlagDefendEvent::class)
//            subclass(WoolPickupEvent::class)
//            subclass(WoolCaptureEvent::class)
//            subclass(WoolDropEvent::class)
//            subclass(WoolDefendEvent::class)
//            subclass(CoreLeakEvent::class)
//            subclass(CoreDamageEvent::class)
//            subclass(DestroyableDestroyEvent::class)
//            subclass(DestroyableDamageEvent::class)
//            subclass(ControlPointCaptureEvent::class)
//        }
//    }
//}

class Handler(val function: KFunction<Unit>, val event: KClass<out ServerEvent>, val priority: EventPriority)

fun Application.initSocketHandler2() {
    val listeners = mutableSetOf<Listener>()
    val handlers = mutableSetOf<Handler>()

    listeners += MatchListener()
    listeners += PartyListener()
    listeners += ChatListener()
    listeners += PlayerStatListener()

    listeners.forEach { listener ->
        listener.handlers.forEach {
            try {
                val priority = it.key.javaMethod!!.getAnnotation(FireAt::class.java).priority
                handlers += Handler(it.key, it.value, priority)
            } catch (ex: Exception) {
                throw RuntimeException("Missing @FireAt annotation on handler method ${it.key.name} in ${listener::class.simpleName}")
            }
        }
    }

    routing {
        webSocket("/minecraft") {
            val serverID = call.request.queryParameters["id"]
            val serverToken = call.request.queryParameters["token"]

            // todo: don't hardcode credentials + support multiple servers
            if (!(serverID == "main" && serverToken == "secret")) throw RuntimeException("Invalid server ID or server token")

            val server = LiveGameServer(serverID, serverToken, this)
            ConnectedServers += server
            println("Server ${server.id} connected to socket server")

            try {
                for (frame in incoming) {
                    frame as? Frame.Binary ?: continue
                    val body = frame.readBytes().zlibDecompress()

                    val json = Json.parseToJsonElement(body).jsonObject
                    val eventName = json["e"]?.jsonPrimitive?.content ?: throw RuntimeException("Invalid event name")
                    val data = json["d"]?.jsonObject ?: throw RuntimeException("Invalid event data")

                    println("$eventName : $data")

                    var event: ServerEvent = when (EventType.valueOf(eventName)) {
                        EventType.MATCH_LOAD -> MatchLoadEvent(server, Json.decodeFromJsonElement(data))
                        EventType.MATCH_START -> MatchStartEvent(
                            server.currentMatch!!,
                            Json.decodeFromJsonElement(data)
                        )
                        EventType.MATCH_END -> MatchEndEvent(server.currentMatch!!, Json.decodeFromJsonElement(data))
                        EventType.PARTY_JOIN -> PartyJoinEvent(server.currentMatch!!, Json.decodeFromJsonElement(data))
                        EventType.PARTY_LEAVE -> PartyLeaveEvent(server.currentMatch!!, Json.decodeFromJsonElement(data))
                        EventType.PLAYER_CHAT -> PlayerChatEvent(server.currentMatch!!, Json.decodeFromJsonElement(data))
                        EventType.PLAYER_DEATH -> PlayerDeathEvent(server.currentMatch!!, Json.decodeFromJsonElement(data))
                        else -> null
                    } ?: throw RuntimeException("Unknown event called")

                    handlers.filter { it.event.isInstance(event) }.sortedBy { it.priority }.forEach {
                        if (event.cancelled) return@forEach
                        it.function.callSuspend(event)
                    }
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