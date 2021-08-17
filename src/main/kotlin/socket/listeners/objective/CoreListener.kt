package network.warzone.api.socket.listeners.objective

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import network.warzone.api.database.realtime.LiveMinecraftServer
import network.warzone.api.database.realtime.MatchEvent
import network.warzone.api.socket.SocketEvent
import network.warzone.api.socket.SocketListener
import network.warzone.api.socket.format
import network.warzone.api.socket.listeners.GoalContribution

object CoreListener : SocketListener() {
    override suspend fun handle(server: LiveMinecraftServer, event: SocketEvent, json: JsonObject) {
        when (event) {
            SocketEvent.CORE_DAMAGE -> onDamage(server, format.decodeFromJsonElement(json))
            SocketEvent.CORE_LEAK -> onLeak(server, format.decodeFromJsonElement(json))
            else -> Unit
        }
    }

    private fun onDamage(server: LiveMinecraftServer, data: CoreDamageData) {
        val current = server.currentMatch ?: return println("No match")
        current.events.add(CoreDamageEvent(data))
        current.save()
    }

    private fun onLeak(server: LiveMinecraftServer, data: CoreLeakData) {
        val current = server.currentMatch ?: return println("No match")
        current.events.add(CoreLeakEvent(data))
        current.save()
    }
}

@Serializable
data class CoreLeakData(val coreId: String, val contributions: Set<GoalContribution>)

@Serializable
@SerialName("CORE_LEAK")
data class CoreLeakEvent(val data: CoreLeakData) : MatchEvent(SocketEvent.CORE_LEAK, System.currentTimeMillis())

@Serializable
data class CoreDamageData(val coreId: String, val playerId: String)

@Serializable
@SerialName("CORE_DAMAGE")
data class CoreDamageEvent(val data: CoreDamageData) : MatchEvent(SocketEvent.CORE_DAMAGE, System.currentTimeMillis())