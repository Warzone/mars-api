package network.warzone.api.http.player

import kotlinx.serialization.Serializable
import network.warzone.api.database.model.Player
import network.warzone.api.database.model.Session

@Serializable
data class PlayerLoginRequest(
    val playerId: String,
    val playerName: String,
    val ip: String
)

@Serializable
data class PlayerLoginResponse(
    val player: Player,
    val activeSession: Session?
)

@Serializable
data class PlayerLogoutRequest(val playerId: String, val playtime: Long)

@Serializable
data class PlayerRanksModifyRequest(val rankId: String)

@Serializable
data class PlayerProfileResponse(val player: Player)