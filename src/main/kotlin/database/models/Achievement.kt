package network.warzone.api.database.models

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import network.warzone.api.database.Database
import network.warzone.api.database.deleteById
import network.warzone.api.database.findById
import org.litote.kmongo.coroutine.CoroutineCollection

enum class AgentType {
    TOTAL_KILLS_AGENT
}

@Serializable
sealed class AgentParams {
    @Serializable
    @SerialName("IntOnly")
    data class IntOnly(val i: Int) : AgentParams()

    @Serializable
    @SerialName("IntAndString")
    data class IntAndString(val i: Int, val s: String) : AgentParams()

    // add more classes as needed for each type of parameter set
}

@Serializable
data class Agent(
    val type: AgentType,
    @Contextual
    val params: AgentParams
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

        //TODO: This currently does not work
        suspend fun deleteAchievement(achievementId: String): Boolean {
            val achievements: CoroutineCollection<Achievement> = Database.database.getCollection()
            val deleteResult = achievements.deleteById(achievementId)
            return deleteResult.wasAcknowledged() && deleteResult.deletedCount > 0
        }

        suspend fun getAchievementCompletions(playerId: String): List<Achievement> {
            // This is a placeholder.
            return emptyList()
        }

        suspend fun findById(achievementId: String): Achievement? {
            val achievements: CoroutineCollection<Achievement> = Database.database.getCollection()
            return achievements.findById(achievementId)
        }
    }
}