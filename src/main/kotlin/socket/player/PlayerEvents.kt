package network.warzone.api.socket.player

import kotlinx.serialization.Serializable
import network.warzone.api.database.models.DamageCause
import network.warzone.api.database.models.SimplePlayer

@Serializable
data class PlayerDeathData(
    val victim: SimplePlayer,
    val attacker: SimplePlayer? = null,
    val weapon: String? = null,
    val entity: String? = null,
    val distance: Int? = null,
    val key: String,
    val cause: DamageCause,
) {
    val isMurder: Boolean
    get() = attacker != null && attacker != victim

    val safeWeapon: String
    get() = if (distance == null) weapon ?: "NONE" else "PROJECTILE"
}

@Serializable
data class PlayerChatData(
    val playerId: String,
    val playerName: String,
    val playerPrefix: String,
    val channel: ChatChannel,
    val message: String,
    val serverId: String
) {
    @Serializable
    enum class ChatChannel {
        STAFF,
        GLOBAL,
        TEAM
    }
}

@Serializable
data class KillstreakData(val amount: Int, val player: SimplePlayer, val ended: Boolean)

@Serializable
data class PartyJoinData(val playerId: String, val playerName: String, val partyName: String)

@Serializable
data class PartyLeaveData(val playerId: String, val playerName: String)

@Serializable
data class MessageData(val message: String, val sound: String?, val playerIds: List<String>)

@Serializable
data class PlayerXPGainData(val playerId: String, val gain: Int, val reason: String, val notify: Boolean)