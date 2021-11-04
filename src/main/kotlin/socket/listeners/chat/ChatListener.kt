package network.warzone.api.socket.listeners.chat

import kotlinx.serialization.Serializable
import network.warzone.api.database.models.Match
import network.warzone.api.socket.event.EventPriority
import network.warzone.api.socket.event.EventType
import network.warzone.api.socket.event.FireAt
import network.warzone.api.socket.event.Listener
import network.warzone.api.socket.listeners.match.MatchEvent
import network.warzone.api.socket.listeners.server.ConnectedServers

class ChatListener : Listener() {
    override val handlers = mapOf(
        ::onStaffChat to PlayerChatEvent::class
    )

    @FireAt(EventPriority.EARLY)
    suspend fun onStaffChat(event: PlayerChatEvent) {
        if (event.data.channel != ChatChannel.STAFF) return
        ConnectedServers.forEach {
            it.call(EventType.PLAYER_CHAT, event.data)
        }
    }
}

class PlayerChatEvent(match: Match, val data: PlayerChatData) : MatchEvent(match) {
    @Serializable
    data class PlayerChatData(val playerId: String, val playerName: String, val playerPrefix: String, val channel: ChatChannel, val message: String, val serverId: String)
}

@Serializable
enum class ChatChannel {
    STAFF,
    GLOBAL,
    TEAM
}