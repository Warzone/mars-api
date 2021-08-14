package network.warzone.api.socket

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import network.warzone.api.database.realtime.LiveMinecraftServer

abstract class SocketListener {
    abstract suspend fun handle(server: LiveMinecraftServer, event: SocketEvent, json: JsonObject)
}

@Serializable
enum class SocketEvent {
    MATCH_LOAD,
    MATCH_START,
    MATCH_END,
    OBJECTIVE_CORE_LEAK,
    OBJECTIVE_FLAG_CAPTURE,
    OBJECTIVE_WOOL_PLACE,
    PLAYER_RECORD_BREAK,
    PLAYER_DEATH,
    PLAYER_CHAT,
    PARTY_JOIN,
    PARTY_LEAVE
}