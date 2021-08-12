package network.warzone.api.socket

import kotlinx.serialization.Serializable
import network.warzone.api.socket.connection.MinecraftConnection

interface MinecraftSocketEvent {
    suspend fun fire(conn: MinecraftConnection)
}

@Serializable
enum class SocketEventType(val rawName: String) {
    MATCH_LOAD("MATCH_LOAD"),
    MATCH_START("MATCH_START"),
    MATCH_END("MATCH_END"),
    OBJECTIVE_CORE_LEAK("OBJECTIVE_CORE_LEAK"),
    OBJECTIVE_FLAG_CAPTURE("OBJECTIVE_FLAG_CAPTURE"),
    OBJECTIVE_WOOL_PLACE("OBJECTIVE_WOOL_PLACE"),
    PLAYER_RECORD_BREAK("PLAYER_RECORD_BREAK"),
    PLAYER_DEATH("PLAYER_DEATH"),
    PLAYER_CHAT("PLAYER_CHAT");

    companion object {
        fun fromRawName(value: String): SocketEventType? = values().find { it.rawName == value }
    }
}