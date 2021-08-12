package network.warzone.api.socket.event.inbound.match

import kotlinx.serialization.Serializable
import network.warzone.api.database.Redis
import network.warzone.api.database.model.Match
import network.warzone.api.socket.MinecraftSocketEvent
import network.warzone.api.socket.connection.MinecraftConnection
import java.util.*

@Serializable
data class MatchLoadEventData(val mapId: String)

class MatchLoadEvent(private val data: MatchLoadEventData) : MinecraftSocketEvent {
    override suspend fun fire(conn: MinecraftConnection) {
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
}