package network.warzone.api.database.models

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import network.warzone.api.database.Database
import org.litote.kmongo.coroutine.CoroutineCollection

enum class AgentType

@Serializable
data class Agent(
    val type: AgentType,
    @Contextual
    val params: Any
)

@Serializable
data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val agent: Agent
) {
    companion object {
        suspend fun addAchievement(achievement: Achievement): String {
            val achievements: CoroutineCollection<Achievement> = Database.database.getCollection()
            achievements.insertOne(achievement)
            return achievement.id
        }

        suspend fun getAchievements(): List<Achievement> {
            val achievements: CoroutineCollection<Achievement> = Database.database.getCollection()
            return achievements.find().toList()
        }

        suspend fun getAchievementCompletions(playerId: String): List<Achievement> {
            // Implementation will vary based on how you're tracking completions.
            // This is a placeholder.
            return emptyList()
        }
    }
}