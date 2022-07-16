package network.warzone.api.http.player

import kotlinx.serialization.Serializable
import network.warzone.api.database.models.Player
import network.warzone.api.database.models.Punishment
import network.warzone.api.database.models.Session
import network.warzone.api.database.models.SimplePlayer
import network.warzone.api.socket.leaderboard.ScoreType

@Serializable
data class PlayerPreLoginRequest(
    val player: SimplePlayer,
    val ip: String
)

@Serializable
data class PlayerPreLoginResponse(
    val new: Boolean,
    val allowed: Boolean,
    val player: Player,
    val activePunishments: List<Punishment>
)

@Serializable
data class PlayerLoginResponse(
    val activeSession: Session,
)

@Serializable
data class PlayerLogoutRequest(val player: SimplePlayer, val sessionId: String, val playtime: Long)

@Serializable
data class PlayerSetActiveTagRequest(val activeTagId: String? = null)

@Serializable
data class PlayerAltResponse(val player: Player, val punishments: List<Punishment>)

@Serializable
data class PlayerLookupResponse(val player: Player, val alts: List<PlayerAltResponse>)

@Serializable
data class PlayerAddNoteRequest(val author: SimplePlayer, val content: String)

@Serializable
data class PlayerProfileResponse(val player: Player, val leaderboardPositions: Map<ScoreType, Long>)