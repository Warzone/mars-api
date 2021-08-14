package network.warzone.api.database.realtime

import io.ktor.websocket.*
import network.warzone.api.database.Redis

data class LiveMinecraftServer(val id: String, val token: String, val session: DefaultWebSocketServerSession) {
    val currentMatch: LiveMatch?
        get() = Redis.get("match:$currentMatchId")

    var currentMatchId: String?
        get() = Redis.get("server:$id:current_match_id")
        set(matchId) = Redis.set("server:$id:current_match_id", matchId)
}