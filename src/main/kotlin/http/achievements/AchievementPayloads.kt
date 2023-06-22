package network.warzone.api.http.achievements

import kotlinx.serialization.Serializable
import network.warzone.api.database.models.Agent

@Serializable
data class AchievementCreateRequest(
    val id: String,
    val name: String,
    val description: String,
    val agent: Agent
)