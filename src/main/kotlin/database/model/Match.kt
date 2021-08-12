package network.warzone.api.database.model

import kotlinx.serialization.Serializable

@Serializable
data class TeamMapping(val playerId: String, val playerName: String, val teamName: String)

@Serializable
data class Match(
    val _id: String,
    val loadedAt: Long,
    var startedAt: Long?,
    var endedAt: Long?,
    val mapId: String,
    var teamMappings: List<TeamMapping>
)