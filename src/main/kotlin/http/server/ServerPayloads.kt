package network.warzone.api.http.server

import kotlinx.serialization.Serializable
import network.warzone.api.database.models.Match
import network.warzone.api.database.models.SimplePlayer

@Serializable
data class ServerStatusResponse(val lastAliveTime: Long, val currentMatch: Match, val onlinePlayers: List<SimplePlayer>, val statsTracking: Boolean)