package network.warzone.api.socket.event.inbound.match

import network.warzone.api.database.Redis
import network.warzone.api.socket.MinecraftSocketEvent
import network.warzone.api.socket.connection.MinecraftConnection

class MatchStartEvent : MinecraftSocketEvent {
    override suspend fun fire(conn: MinecraftConnection) {
        val current = Redis.getCurrentMatch(conn.serverInfo.id) ?: return println("Unloaded match tried to start?") // just log/stop execution for now

        if (current.startedAt !== null) return println("In-progress match is trying to start?") // log/stop exec for now

        current.startedAt = System.currentTimeMillis()

        Redis.setCurrentMatch(conn.serverInfo.id, current)
    }
}