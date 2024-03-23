package network.warzone.api.database.models

import com.mongodb.client.model.Projections
import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.UpdateResult
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import network.warzone.api.database.*
import org.bson.BsonDocument
import org.bson.Document
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.CoroutineFindPublisher

enum class RecordType {
    LONGEST_SESSION,
    LONGEST_PROJECTILE_KILL,
    FASTEST_WOOL_CAPTURE,
    FASTEST_FLAG_CAPTURE,
    FASTEST_FIRST_BLOOD,
    KILLS_IN_MATCH,
    DEATHS_IN_MATCH
}

enum class AgentType {
    TOTAL_KILLS_AGENT, /** WORKS **/
    TOTAL_DEATHS_AGENT, /** WORKS **/
    TOTAL_WINS_AGENT, /** WORKS **/
    TOTAL_LOSSES_AGENT, /** WORKS **/
    KILL_STREAK_AGENT, /** WORKS **/
    FIRE_DEATH_AGENT, /** WORKS **/
    CHAT_MESSAGE_AGENT, /** WORKS **/
    LEVEL_UP_AGENT, /** WORKS **/
    CAPTURE_NO_SPRINT_AGENT, /** DOESN'T WORK **/
    WOOL_CAPTURE_AGENT, /** WORKS **/
    FIRST_BLOOD_AGENT, /** WORKS **/
    BOW_DISTANCE_AGENT, /** WORKS **/
    FLAG_CAPTURE_AGENT, /** WORKS **/
    FLAG_DEFEND_AGENT, /** WORKS **/
    WOOL_DEFEND_AGENT, /** DOESN'T WORK **/
    MONUMENT_DAMAGE_AGENT, /** WORKS **/
    KILL_CONSECUTIVE_AGENT, /** WORKS **/
    PLAY_TIME_AGENT, /** WORKS **/
    RECORD_AGENT, /** DOESN'T WORK **/
    CONTROL_POINT_CAPTURE_AGENT, /** WORKS **/
    //COMPOSITE_AGENT
}

@Serializable
sealed class AchievementCategory {
    abstract val category: String
    abstract val displayName: String
    abstract val description: String

    @Serializable
    @SerialName("NoCategory")
    data class NoCategory(
        override val category: String = "Misc",
        override val displayName: String = "No Display Name",
        override val description: String = "No Description",
    ) : AchievementCategory()

    // Obtain a killstreak of "x"
    @Serializable
    @SerialName("BloodBathCategory")
    data class BloodBathCategory(
        override val category: String = "Kills",
        override val displayName: String = "Blood Bath",
        override val description: String = "Click here to view this achievement."
    ) : AchievementCategory()

    // Capture "x" wools
    @Serializable
    @SerialName("WoolieMammothCategory")
    data class WoolieMammothCategory(
        override val category: String = "Objectives",
        override val displayName: String = "Woolie Mammoth",
        override val description: String = "Click here to view this achievement."
    ) : AchievementCategory()

    // Obtain "x" total kills
    @Serializable
    @SerialName("PathToGenocideCategory")
    data class PathToGenocideCategory(
        override val category: String = "Kills",
        override val displayName: String = "Path to Genocide",
        override val description: String = "Click here to view this achievement."
    ) : AchievementCategory()

    // Shoot and kill a player from "x" blocks away
    @Serializable
    @SerialName("MarksmanCategory")
    data class MarksmanCategory(
        override val category: String = "Kills",
        override val displayName: String = "Marksman",
        override val description: String = "Click here to view this achievement."
    ) : AchievementCategory()

    // Kill "x" players within a span of "y" seconds.
    @Serializable
    @SerialName("MercilessCategory")
    data class MercilessCategory(
        override val category: String = "Kills",
        override val displayName: String = "Merciless",
        override val description: String = "Click here to view this achievement."
    ) : AchievementCategory()

    // Kill "x" players within "y" seconds of each kill.
    @Serializable
    @SerialName("WomboComboCategory")
    data class WomboComboCategory(
        override val category: String = "Kills",
        override val displayName: String = "Wombo Combo",
        override val description: String = "Click here to view this achievement."
    ) : AchievementCategory()

    // Die to a source of fire.
    @Serializable
    @SerialName("BurntToastCategory")
    data class BurntToastCategory(
        override val category: String = "Deaths",
        override val displayName: String = "Burnt Toast",
        override val description: String = "Click here to view this achievement."
    ) : AchievementCategory()

    // Obtain your first first-blood kill.
    @Serializable
    @SerialName("BloodGodCategory")
    data class BloodGodCategory(
        override val category: String = "Kills",
        override val displayName: String = "Blood God",
        override val description: String = "Click here to view this achievement."
    ) : AchievementCategory()

    // Obtain "x" first-blood kills.
    @Serializable
    @SerialName("TotalFirstBloodsCategory")
    data class TotalFirstBloodsCategory(
        override val category: String = "Kills",
        override val displayName: String = "Swift as the Wind",
        override val description: String = "Click here to view this achievement."
    ) : AchievementCategory()

    // Damage "x" monument blocks overall.
    @Serializable
    @SerialName("PillarsOfSandCategory")
    data class PillarsOfSandCategory(
        override val category: String = "Objectives",
        override val displayName: String = "Pillars of Sand",
        override val description: String = "Click here to view this achievement."
    ) : AchievementCategory()

    // Capture "x" flags overall.
    @Serializable
    @SerialName("TouchdownCategory")
    data class TouchdownCategory(
        override val category: String = "Objectives",
        override val displayName: String = "Touchdown",
        override val description: String = "Click here to view this achievement."
    ) : AchievementCategory()

    // Stop "x" flag holders from capturing the flag.
    @Serializable
    @SerialName("PassInterferenceCategory")
    data class PassInterferenceCategory(
        override val category: String = "Objectives",
        override val displayName: String = "Pass Interference",
        override val description: String = "Click here to view this achievement."
    ) : AchievementCategory()

    // Capture "x" control-point objectives overall.
    @Serializable
    @SerialName("TerritorialDisputeCategory")
    data class TerritorialDisputeCategory(
        override val category: String = "Objectives",
        override val displayName: String = "Territorial Dispute",
        override val description: String = "Click here to view this achievement."
    ) : AchievementCategory()

    // Win "x" matches overall.
    @Serializable
    @SerialName("VictoryScreechCategory")
    data class VictoryScreechCategory(
        override val category: String = "Wins",
        override val displayName: String = "Victory Screech",
        override val description: String = "Click here to view this achievement."
    ) : AchievementCategory()

    // Reach level "x".
    @Serializable
    @SerialName("ChampionRoadCategory")
    data class ChampionRoadCategory(
        override val category: String = "Misc",
        override val displayName: String = "Champion Road",
        override val description: String = "Click here to view this achievement."
    ) : AchievementCategory()

    // Play for "x" hours in matches overall.
    @Serializable
    @SerialName("TouchGrassCategory")
    data class TouchGrassCategory(
        override val category: String = "Misc",
        override val displayName: String = "Touch Grass",
        override val description: String = "Click here to view this achievement."
    ) : AchievementCategory()

    // Win your first match.
    @Serializable
    @SerialName("FirstWinCategory")
    data class FirstWinCategory(
        override val category: String = "Wins",
        override val displayName: String = "Mom, Get the Camera!",
        override val description: String = "Click here to view this achievement."
    ) : AchievementCategory()

    // Obtain your first kill.
    @Serializable
    @SerialName("FirstKillCategory")
    data class FirstKillCategory(
        override val category: String = "Kills",
        override val displayName: String = "Baby Steps",
        override val description: String = "Click here to view this achievement."
    ) : AchievementCategory()

    // Obtain your first loss.
    @Serializable
    @SerialName("FirstLossCategory")
    data class FirstLossCategory(
        override val category: String = "Losses",
        override val displayName: String = "My Stats!",
        override val description: String = "Click here to view this achievement."
    ) : AchievementCategory()

    // Obtain your first death.
    @Serializable
    @SerialName("FirstDeathCategory")
    data class FirstDeathCategory(
        override val category: String = "Deaths",
        override val displayName: String = "Oof!",
        override val description: String = "Click here to view this achievement."
    ) : AchievementCategory()

    // ... other subclasses here ...
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
    @SerialName("FireDeathAgentParams")
    object FireDeathAgentParams : AgentParams()

    @Serializable
    @SerialName("CaptureNoSprintAgentParams")
    object CaptureNoSprintAgentParams : AgentParams()

    @Serializable
    @SerialName("CompositeAgentParams")
    data class CompositeAgentParams(val agents: List<Agent>) : AgentParams()

    @Serializable
    @SerialName("ChatMessageAgentParams")
    data class ChatMessageAgentParams(val message: String) : AgentParams()

    @Serializable
    @SerialName("LevelUpAgentParams")
    data class LevelUpAgentParams(val level: Int) : AgentParams()

    @Serializable
    @SerialName("WoolCaptureAgentParams")
    data class WoolCaptureAgentParams(val captures: Int) : AgentParams()

    @Serializable
    @SerialName("FirstBloodAgentParams")
    data class FirstBloodAgentParams(val target: Int) : AgentParams()

    @Serializable
    @SerialName("BowDistanceAgentParams")
    data class BowDistanceAgentParams(val distance: Long) : AgentParams()

    @Serializable
    @SerialName("FlagCaptureAgentParams")
    data class FlagCaptureAgentParams(val captures: Int) : AgentParams()

    @Serializable
    @SerialName("FlagDefendAgentParams")
    data class FlagDefendAgentParams(val defends: Int) : AgentParams()

    @Serializable
    @SerialName("WoolDefendAgentParams")
    data class WoolDefendAgentParams(val defends: Int) : AgentParams()

    @Serializable
    @SerialName("MonumentDamageAgentParams")
    data class MonumentDamageAgentParams(val breaks: Int) : AgentParams()

    @Serializable
    @SerialName("MonumentDestroyAgentParams")
    data class MonumentDestroyAgentParams(val destroys: Int) : AgentParams()

    @Serializable
    @SerialName("KillConsecutiveAgentParams")
    data class KillConsecutiveAgentParams(val seconds: Long, val kills: Int, val allWithin: Boolean) : AgentParams()

    @Serializable
    @SerialName("PlayTimeAgentParams")
    data class PlayTimeAgentParams(val hours: Long) : AgentParams()

    @Serializable
    @SerialName("RecordAgentParams")
    data class RecordAgentParams<T : Number>(val recordType: RecordType, val threshold: T) : AgentParams()

    @Serializable
    @SerialName("ControlPointCaptureAgentParams")
    data class ControlPointCaptureAgentParams(val captures: Int) : AgentParams()

    @Serializable
    @SerialName("TotalWinsAgentParams")
    data class TotalWinsAgentParams(val wins: Int) : AgentParams()

    @Serializable
    @SerialName("TotalDeathsAgentParams")
    data class TotalDeathsAgentParams(val deaths: Int) : AgentParams()

    @Serializable
    @SerialName("TotalLossesAgentParams")
    data class TotalLossesAgentParams(val losses: Int) : AgentParams()
}

@Serializable
data class Agent(
    val type: AgentType,
    @Contextual
    @Serializable
    val params: AgentParams? = null
)

@Serializable
data class AchievementStatistic(
    val completionTime: Long
)

@Serializable
data class Achievement(
    val _id: String,
    val name: String,
    val description: String,
    @Serializable
    val category: AchievementCategory? = null,
    val agent: Agent,
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
            if (achievementId == "*") {
                deleteResult = achievements.deleteMany(BsonDocument())
                println("${deleteResult.deletedCount} documents were deleted.")
            }
            else {
                deleteResult = achievements.deleteById(achievementId)
            }
            removeAchievementFromAllPlayers(achievementId)
            return deleteResult.wasAcknowledged() && deleteResult.deletedCount > 0
        }

        suspend fun removeAchievementFromAllPlayers(achievementId: String) {
            val updateResult: UpdateResult
            if (achievementId == "*") {
                val query = and(
                    (Player::stats / PlayerStats::achievements).exists(),
                    not((Player::stats / PlayerStats::achievements).eq(emptyMap()))
                )
                val playersWithNonEmptyAchievements = Database.players.find(query).toList()

                // Database Removal
                updateResult = Database.players.updateMany(
                    query,  // This will match all players
                    Document("\$set", Document("stats.achievements", mapOf<String, AchievementStatistic>()))  // Clear the achievements array
                )

                // Cache Removal
                for (player in playersWithNonEmptyAchievements) {
                    player.stats.achievements.clear()
                    PlayerCache.set(player.name, player, false)
                }

            } else {
                val query = (Player::stats / PlayerStats::achievements.keyProjection(achievementId)).exists()
                val playersWithAchievement = Database.players.find(query).toList()

                // Database Removal
                updateResult = Database.players.updateMany(
                    query,
                    Document("\$unset", Document("stats.achievements.$achievementId", ""))  // Remove the specific achievement ID
                )

                // Cache Removal
                for (player in playersWithAchievement) {
                    player.stats.achievements.remove(achievementId)
                    PlayerCache.set(player.name, player, false)
                }
            }
        }

        suspend fun findById(achievementId: String): Achievement? {
            val achievements: CoroutineCollection<Achievement> = Database.database.getCollection()
            return achievements.findById(achievementId)
        }
    }
}