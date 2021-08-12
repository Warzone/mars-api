package network.warzone.api.socket.listeners

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import network.warzone.api.database.Redis
import network.warzone.api.database.models.Match
import network.warzone.api.socket.MinecraftConnection
import network.warzone.api.socket.SocketEvent
import network.warzone.api.socket.SocketListener
import java.util.*

object MatchListener : SocketListener() {
    override suspend fun handle(conn: MinecraftConnection, event: SocketEvent, json: JsonObject) {
        when (event) {
            SocketEvent.MATCH_LOAD -> onLoad(conn, Json.decodeFromJsonElement(json))
            SocketEvent.MATCH_START -> onStart(conn)
            else -> Unit
        }
    }

    private fun onLoad(conn: MinecraftConnection, data: MatchLoadData) {
        val match = Match(
            _id = UUID.randomUUID().toString(),
            loadedAt = System.currentTimeMillis(),
            startedAt = null,
            endedAt = null,
            mapId = data.mapId,
            teamMappings = emptyList()
        )

        Redis.setCurrentMatch(conn.serverInfo.id, match)
    }

    private fun onStart(conn: MinecraftConnection) {
        val current = Redis.getCurrentMatch(conn.serverInfo.id) ?: return println("Unloaded match tried to start?") // just log/stop execution for now

        if (current.startedAt !== null) return println("In-progress match is trying to start?") // log/stop exec for now

        current.startedAt = System.currentTimeMillis()

        Redis.setCurrentMatch(conn.serverInfo.id, current)
    }
}

@Serializable
private data class MatchLoadData(val mapId: String)