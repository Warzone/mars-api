package network.warzone.api.socket

import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import network.warzone.api.socket.event.*
import network.warzone.api.socket.listeners.chat.ChatListener
import network.warzone.api.socket.listeners.chat.PlayerChatEvent
import network.warzone.api.socket.listeners.death.DeathListener
import network.warzone.api.socket.listeners.death.PlayerDeathEvent
import network.warzone.api.socket.listeners.match.MatchEndEvent
import network.warzone.api.socket.listeners.match.MatchLoadEvent
import network.warzone.api.socket.listeners.match.MatchPhaseListener
import network.warzone.api.socket.listeners.match.MatchStartEvent
import network.warzone.api.socket.listeners.objective.*
import network.warzone.api.socket.listeners.party.PartyJoinEvent
import network.warzone.api.socket.listeners.party.PartyLeaveEvent
import network.warzone.api.socket.listeners.party.PartyListener
import network.warzone.api.socket.listeners.server.ConnectedServers
import network.warzone.api.socket.listeners.server.LiveGameServer
import network.warzone.api.socket.listeners.stats.GamemodeStatListener
import network.warzone.api.socket.listeners.stats.KillstreakEvent
import network.warzone.api.socket.listeners.stats.ParticipantStatListener
import network.warzone.api.socket.listeners.stats.PlayerStatListener
import network.warzone.api.util.zlibDecompress
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.jvm.javaMethod

class Handler(val function: KFunction<Unit>, val event: KClass<out ServerEvent>, val priority: EventPriority)

fun Application.initSocketHandler() {
    val listeners = mutableSetOf<Listener>()
    val handlers = mutableSetOf<Handler>()

    listeners += MatchPhaseListener()
    listeners += PartyListener()
    listeners += ChatListener()
    listeners += DeathListener()
    listeners += ParticipantStatListener()
    listeners += PlayerStatListener()
    listeners += GamemodeStatListener()

    listeners.forEach { listener ->
        listener.handlers.forEach {
            try {
                val method = it.key.javaMethod ?: throw RuntimeException("Listener handler key java method is null")
                val priority = method.getAnnotation(FireAt::class.java).priority
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
                    println(body)

                    val json = Json.parseToJsonElement(body).jsonObject
                    val eventName = json["e"]?.jsonPrimitive?.content ?: throw RuntimeException("Invalid event name")
                    val data = json["d"]?.jsonObject ?: throw RuntimeException("Invalid event data")

                    println("$eventName : $data")

                    val match = server.currentMatch

                    val eventType = EventType.valueOf(eventName)

                    val event = if (eventType == EventType.MATCH_LOAD) {
                        MatchLoadEvent(server, Json.decodeFromJsonElement(data))
                    } else if (match != null) {
                        when (eventType) {
                            EventType.MATCH_LOAD -> MatchLoadEvent(server, Json.decodeFromJsonElement(data))
                            EventType.MATCH_START -> MatchStartEvent(
                                match,
                                Json.decodeFromJsonElement(data)
                            )
                            EventType.MATCH_END -> MatchEndEvent(match, Json.decodeFromJsonElement(data))
                            EventType.PARTY_JOIN -> PartyJoinEvent(match, Json.decodeFromJsonElement(data))
                            EventType.PARTY_LEAVE -> PartyLeaveEvent(match, Json.decodeFromJsonElement(data))
                            EventType.PLAYER_CHAT -> PlayerChatEvent(match, Json.decodeFromJsonElement(data))
                            EventType.PLAYER_DEATH -> PlayerDeathEvent(match, Json.decodeFromJsonElement(data))
                            EventType.KILLSTREAK -> KillstreakEvent(match, Json.decodeFromJsonElement(data))
                            EventType.CONTROL_POINT_CAPTURE -> ControlPointCaptureEvent(
                                match,
                                Json.decodeFromJsonElement(data)
                            )
                            EventType.CORE_LEAK -> CoreLeakEvent(match, Json.decodeFromJsonElement(data))
                            EventType.CORE_DAMAGE -> CoreDamageEvent(match, Json.decodeFromJsonElement(data))
                            EventType.DESTROYABLE_DAMAGE -> DestroyableDamageEvent(
                                match,
                                Json.decodeFromJsonElement(data)
                            )
                            EventType.DESTROYABLE_DESTROY -> DestroyableDestroyEvent(
                                match,
                                Json.decodeFromJsonElement(data)
                            )
                            EventType.FLAG_PICKUP -> FlagPickupEvent(match, Json.decodeFromJsonElement(data))
                            EventType.FLAG_CAPTURE -> FlagPlaceEvent(match, Json.decodeFromJsonElement(data))
                            EventType.FLAG_DEFEND -> FlagDefendEvent(match, Json.decodeFromJsonElement(data))
                            EventType.FLAG_DROP -> FlagDropEvent(match, Json.decodeFromJsonElement(data))
                            EventType.WOOL_PICKUP -> WoolPickupEvent(match, Json.decodeFromJsonElement(data))
                            EventType.WOOL_CAPTURE -> WoolPlaceEvent(match, Json.decodeFromJsonElement(data))
                            EventType.WOOL_DEFEND -> WoolDefendEvent(match, Json.decodeFromJsonElement(data))
                            EventType.WOOL_DROP -> WoolDropEvent(match, Json.decodeFromJsonElement(data))
                            else -> throw RuntimeException("Event $eventType called by server ${server.id} was not handled")
                        }
                    } else {
                        println("No match on server ${server.id} but received event type $eventType")
                        continue
                    }

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