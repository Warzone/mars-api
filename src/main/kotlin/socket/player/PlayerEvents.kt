package network.warzone.api.socket.player

import kotlinx.serialization.Serializable
import network.warzone.api.database.models.DamageCause
import network.warzone.api.database.models.SimplePlayer

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
    val cause: DamageCause,
    val attackerKillstreak: Int? = null,
    val victimKillstreakEnded: Int? = null,
) {
    val isMurder: Boolean
    get() = simpleAttacker != null && simpleAttacker != simpleVictim

    val simpleAttacker: SimplePlayer?
    get() {
        if (attackerName == null || attackerId == null) return null
        return SimplePlayer(name = attackerName, id = attackerId)
    }

    val simpleVictim: SimplePlayer
    get() = SimplePlayer(name = victimName, id = victimId)
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