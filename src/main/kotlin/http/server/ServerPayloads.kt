package network.warzone.api.http.server

import kotlinx.serialization.Serializable
import network.warzone.api.database.models.Match

@Serializable
data class ServerStatusResponse(val lastAliveTime: Long, val currentMatch: Match, val statsTracking: Boolean)