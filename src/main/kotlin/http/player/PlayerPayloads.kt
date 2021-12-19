package network.warzone.api.http.player

import kotlinx.serialization.Serializable
import network.warzone.api.database.models.Player
import network.warzone.api.database.models.Punishment
import network.warzone.api.database.models.Session
import network.warzone.api.database.models.SimplePlayer

@Serializable
data class PlayerLoginRequest(
    val playerId: String,
    val playerName: String,
    val ip: String
)

@Serializable
data class PlayerLoginResponse(
    val player: Player,
    val activeSession: Session?,
    val activePunishments: List<Punishment>,
)

@Serializable
data class PlayerLogoutRequest(val playerId: String, val playtime: Long)

@Serializable
data class PlayerSetActiveTagRequest(val activeTagId: String? = null)

@Serializable
data class PlayerAltResponse(val player: Player, val punishments: List<Punishment>)

@Serializable
data class PlayerLookupResponse(val player: Player, val alts: List<PlayerAltResponse>)

@Serializable
data class PlayerAddNoteRequest(val author: SimplePlayer, val content: String)