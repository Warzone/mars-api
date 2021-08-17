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
    PLAYER_RECORD_BREAK,
    PLAYER_DEATH,
    PLAYER_CHAT,
    PARTY_JOIN,
    PARTY_LEAVE,
    DESTROYABLE_DESTROY,
    DESTROYABLE_DAMAGE,
    CORE_LEAK,
    CORE_DAMAGE,
    FLAG_CAPTURE,
    FLAG_PICKUP,
    FLAG_DROP,
    FLAG_DEFEND,
    WOOL_CAPTURE,
    WOOL_PICKUP,
    WOOL_DROP,
    WOOL_DEFEND,
    CONTROL_POINT_CAPTURE,
}