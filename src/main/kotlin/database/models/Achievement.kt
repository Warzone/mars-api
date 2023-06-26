package network.warzone.api.database.models

import com.mongodb.client.result.DeleteResult
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import network.warzone.api.database.Database
import network.warzone.api.database.deleteById
import network.warzone.api.database.findById
import org.bson.BsonDocument
import org.litote.kmongo.coroutine.CoroutineCollection

enum class AgentType {
    TOTAL_KILLS_AGENT,
    KILL_STREAK_AGENT,
    COMPOSITE_AGENT
}

@Serializable
sealed class AgentParams {
    @Serializable
    @SerialName("TotalKillsAgentParams")
    data class TotalKillsAgentParams(val targetKills: Int) : AgentParams()

    @Serializable
    @SerialName("KillStreakAgentParams")
    data class KillStreakAgentParams(val targetStreak: Int) : AgentParams()

    @Serializable
    @SerialName("CompositeAgentParams")
    data class CompositeAgentParams(val agents: List<Agent>) : AgentParams()

    // add more classes as needed for each type of parameter set
}

@Serializable
data class Agent(
    val type: AgentType,
    @Contextual
    val params: AgentParams? = null
)

@Serializable
data class Achievement(
    val _id: String,
    val name: String,
    val description: String,
    val agent: Agent
) {
    companion object {
        suspend fun addAchievement(achievement: Achievement): String {
            val achievements: CoroutineCollection<Achievement> = Database.database.getCollection()
            achievements.insertOne(achievement)
            return achievement._id
        }

        suspend fun getAchievements(): List<Achievement> {
            val achievements: CoroutineCollection<Achievement> = Database.database.getCollection()
            return achievements.find().toList()
        }

        suspend fun deleteAchievement(achievementId: String): Boolean {
            val achievements: CoroutineCollection<Achievement> = Database.database.getCollection()
            val deleteResult: DeleteResult
            // TODO: Remove this branch in the final commit; it's just a debug branch
            //  for me to quickly clear my achievements
            if (achievementId == "*") {
                deleteResult = achievements.deleteMany(BsonDocument())
                println("${deleteResult.deletedCount} documents were deleted.")
            }
            else {
                deleteResult = achievements.deleteById(achievementId)
            }
            return deleteResult.wasAcknowledged() && deleteResult.deletedCount > 0
        }

        suspend fun findById(achievementId: String): Achievement? {
            val achievements: CoroutineCollection<Achievement> = Database.database.getCollection()
            return achievements.findById(achievementId)
        }
    }
}