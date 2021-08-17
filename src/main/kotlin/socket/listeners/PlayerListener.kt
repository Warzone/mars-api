package network.warzone.api.socket.listeners

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import network.warzone.api.database.realtime.LiveMinecraftServer
import network.warzone.api.database.realtime.MatchEvent
import network.warzone.api.socket.ConnectionStore
import network.warzone.api.socket.SocketEvent
import network.warzone.api.socket.SocketListener
import network.warzone.api.socket.format

object PlayerListener : SocketListener() {
    override suspend fun handle(server: LiveMinecraftServer, event: SocketEvent, json: JsonObject) {
        when (event) {
            SocketEvent.PLAYER_DEATH -> onDeath(server, format.decodeFromJsonElement(json))
            SocketEvent.PLAYER_CHAT -> onChat(server, format.decodeFromJsonElement(json))
            else -> Unit
        }
    }

    private fun onDeath(server: LiveMinecraftServer, data: PlayerDeathData) {
        val current =
            server.currentMatch ?: return println("Player died in non-existent match? ${data.victimName}")
//        val victim = current.participants[data.victimId] ?: return println("Victim not found")
        current.events.add(PlayerDeathEvent(data))
        current.save()
    }

    private suspend fun onChat(server: LiveMinecraftServer, data: InboundPlayerChatData) {
        if (data.channel == ChatChannel.STAFF) {
            val newData = OutboundPlayerChatData(
                data.channel,
                data.playerName,
                data.playerId,
                data.message,
                server.id,
                data.playerPrefix
            )
            ConnectionStore.minecraftServers.filter { it.id != server.id }
                .forEach { it.call(SocketEvent.PLAYER_CHAT, newData) }
        } else { // Team or global chat can be added to the Match
            val current = server.currentMatch ?: return println("No current match")
            val event = PlayerChatEvent(data)
            current.events.add(event)
            current.save()
        }
    }
}

@Serializable
data class PlayerDeathData(
    val victimId: String,
    val victimName: String,
    val attackerId: String? = null,
    val attackerName: String? = null,
    val weapon: String? = null,
    val entity: String? = null,
    val distance: Int? = null,
    val key: String,
    val cause: DamageCause
)

@Serializable
@SerialName("PLAYER_DEATH")
data class PlayerDeathEvent(val data: PlayerDeathData) :
    MatchEvent(SocketEvent.PLAYER_DEATH, System.currentTimeMillis())

@Serializable
enum class ChatChannel {
    STAFF,
    GLOBAL,
    TEAM
}

@Serializable
data class InboundPlayerChatData(
    val channel: ChatChannel,
    val playerName: String,
    val playerId: String,
    val message: String,
    val playerPrefix: String
)

@Serializable
@SerialName("PLAYER_CHAT")
data class PlayerChatEvent(val data: InboundPlayerChatData) :
    MatchEvent(SocketEvent.PLAYER_CHAT, System.currentTimeMillis())

@Serializable
data class OutboundPlayerChatData(
    val channel: ChatChannel,
    val playerName: String,
    val playerId: String,
    val message: String,
    val serverId: String,
    val playerPrefix: String
)