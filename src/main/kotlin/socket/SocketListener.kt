package network.warzone.api.socket

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

abstract class SocketListener {
    abstract suspend fun handle(conn: MinecraftConnection, event: SocketEvent, json: JsonObject)
}

@Serializable
enum class SocketEvent(val rawName: String) {
    MATCH_LOAD("MATCH_LOAD"),
    MATCH_START("MATCH_START"),
    MATCH_END("MATCH_END"),
    OBJECTIVE_CORE_LEAK("OBJECTIVE_CORE_LEAK"),
    OBJECTIVE_FLAG_CAPTURE("OBJECTIVE_FLAG_CAPTURE"),
    OBJECTIVE_WOOL_PLACE("OBJECTIVE_WOOL_PLACE"),
    PLAYER_RECORD_BREAK("PLAYER_RECORD_BREAK"),
    PLAYER_DEATH("PLAYER_DEATH"),
    PLAYER_CHAT("PLAYER_CHAT");
    // todo: join match/team event

    companion object {
        fun fromRawName(value: String): SocketEvent? = values().find { it.rawName == value }
    }
}