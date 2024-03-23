package network.warzone.api.http.achievements

import kotlinx.serialization.Serializable
import network.warzone.api.database.models.AchievementCategory
import network.warzone.api.database.models.Agent

@Serializable
data class AchievementCreateRequest(
    val name: String,
    val description: String,
    val category: AchievementCategory? = null,
    val agent: Agent
)

@Serializable
data class AchievementCompletionEvent(
    val playerId: String,
    val achievementId: String
)