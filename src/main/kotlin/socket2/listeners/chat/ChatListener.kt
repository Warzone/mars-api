package network.warzone.api.socket2.listeners.chat

import kotlinx.serialization.Serializable
import network.warzone.api.socket2.event.EventPriority
import network.warzone.api.socket2.event.EventType
import network.warzone.api.socket2.event.FireAt
import network.warzone.api.socket2.event.Listener
import network.warzone.api.socket2.listeners.match.LiveMatch
import network.warzone.api.socket2.listeners.match.MatchEvent
import network.warzone.api.socket2.listeners.server.ConnectedServers

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

class PlayerChatEvent(match: LiveMatch, val data: PlayerChatData) : MatchEvent(match) {
    @Serializable
    data class PlayerChatData(val playerId: String, val playerName: String, val playerPrefix: String, val channel: ChatChannel, val message: String, val serverId: String)
}

@Serializable
enum class ChatChannel {
    STAFF,
    GLOBAL,
    TEAM
}