package network.warzone.api.database.model

import kotlinx.serialization.Serializable

@Serializable
data class TeamMapping(val playerId: String, val playerName: String, val teamName: String)

@Serializable
data class Match(
    val _id: String,
    val loadedAt: Long,
    val startedAt: Long?,
    val endedAt: Long?,
    val mapId: String,
    val teamMappings: List<TeamMapping>
)