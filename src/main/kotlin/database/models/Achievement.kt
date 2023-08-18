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
import java.util.*

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
sealed class AchievementParent {
    abstract val category: String
    abstract val displayName: String
    abstract val description: String

    @Serializable
    @SerialName("NoParent")
    data class NoParent(
        override val category: String = "Misc",
        override val displayName: String = "No Display Name",
        override val description: String = "No Description",
    ) : AchievementParent()

    // Obtain a killstreak of "x"
    @Serializable
    @SerialName("BloodBathParent")
    data class BloodBathParent(
        override val category: String = "Kills",
        override val displayName: String = "Blood Bath",
        override val description: String = "Click here to view this achievement."
    ) : AchievementParent()

    // Capture "x" wools
    @Serializable
    @SerialName("WoolieMammothParent")
    data class WoolieMammothParent(
        override val category: String = "Objectives",
        override val displayName: String = "Woolie Mammoth",
        override val description: String = "Click here to view this achievement."
    ) : AchievementParent()

    // Obtain "x" total kills
    @Serializable
    @SerialName("PathToGenocideParent")
    data class PathToGenocideParent(
        override val category: String = "Kills",
        override val displayName: String = "Path to Genocide",
        override val description: String = "Click here to view this achievement."
    ) : AchievementParent()

    // Shoot and kill a player from "x" blocks away
    @Serializable
    @SerialName("MarksmanParent")
    data class MarksmanParent(
        override val category: String = "Kills",
        override val displayName: String = "Marksman",
        override val description: String = "Click here to view this achievement."
    ) : AchievementParent()

    // Kill "x" players within a span of "y" seconds.
    @Serializable
    @SerialName("MercilessParent")
    data class MercilessParent(
        override val category: String = "Kills",
        override val displayName: String = "Merciless",
        override val description: String = "Click here to view this achievement."
    ) : AchievementParent()

    // Kill "x" players within "y" seconds of each kill.
    @Serializable
    @SerialName("WomboComboParent")
    data class WomboComboParent(
        override val category: String = "Kills",
        override val displayName: String = "Wombo Combo",
        override val description: String = "Click here to view this achievement."
    ) : AchievementParent()

    // Die to a source of fire.
    @Serializable
    @SerialName("BurntToastParent")
    data class BurntToastParent(
        override val category: String = "Deaths",
        override val displayName: String = "Burnt Toast",
        override val description: String = "Click here to view this achievement."
    ) : AchievementParent()

    // Obtain your first first-blood kill.
    @Serializable
    @SerialName("BloodGodParent")
    data class BloodGodParent(
        override val category: String = "Kills",
        override val displayName: String = "Blood God",
        override val description: String = "Click here to view this achievement."
    ) : AchievementParent()

    // Obtain "x" first-blood kills.
    @Serializable
    @SerialName("TotalFirstBloodsParent")
    data class TotalFirstBloodsParent(
        override val category: String = "Kills",
        override val displayName: String = "Swift as the Wind",
        override val description: String = "Click here to view this achievement."
    ) : AchievementParent()

    // Damage "x" monument blocks overall.
    @Serializable
    @SerialName("PillarsOfSandParent")
    data class PillarsOfSandParent(
        override val category: String = "Objectives",
        override val displayName: String = "Pillars of Sand",
        override val description: String = "Click here to view this achievement."
    ) : AchievementParent()

    // Capture "x" flags overall.
    @Serializable
    @SerialName("TouchdownParent")
    data class TouchdownParent(
        override val category: String = "Objectives",
        override val displayName: String = "Touchdown",
        override val description: String = "Click here to view this achievement."
    ) : AchievementParent()

    // Stop "x" flag holders from capturing the flag.
    @Serializable
    @SerialName("PassInterferenceParent")
    data class PassInterferenceParent(
        override val category: String = "Objectives",
        override val displayName: String = "Pass Interference",
        override val description: String = "Click here to view this achievement."
    ) : AchievementParent()

    // Capture "x" control-point objectives overall.
    @Serializable
    @SerialName("TerritorialDisputeParent")
    data class TerritorialDisputeParent(
        override val category: String = "Objectives",
        override val displayName: String = "Territorial Dispute",
        override val description: String = "Click here to view this achievement."
    ) : AchievementParent()

    // Win "x" matches overall.
    @Serializable
    @SerialName("VictoryScreechParent")
    data class VictoryScreechParent(
        override val category: String = "Wins",
        override val displayName: String = "Victory Screech",
        override val description: String = "Click here to view this achievement."
    ) : AchievementParent()

    // Reach level "x".
    @Serializable
    @SerialName("ChampionRoadParent")
    data class ChampionRoadParent(
        override val category: String = "Misc",
        override val displayName: String = "Champion Road",
        override val description: String = "Click here to view this achievement."
    ) : AchievementParent()

    // Play for "x" hours in matches overall.
    @Serializable
    @SerialName("TouchGrassParent")
    data class TouchGrassParent(
        override val category: String = "Misc",
        override val displayName: String = "Touch Grass",
        override val description: String = "Click here to view this achievement."
    ) : AchievementParent()

    // Win your first match.
    @Serializable
    @SerialName("FirstWinParent")
    data class FirstWinParent(
        override val category: String = "Wins",
        override val displayName: String = "Mom, Get the Camera!",
        override val description: String = "Click here to view this achievement."
    ) : AchievementParent()

    // Obtain your first kill.
    @Serializable
    @SerialName("FirstKillParent")
    data class FirstKillParent(
        override val category: String = "Kills",
        override val displayName: String = "Baby Steps",
        override val description: String = "Click here to view this achievement."
    ) : AchievementParent()

    // Obtain your first loss.
    @Serializable
    @SerialName("FirstLossParent")
    data class FirstLossParent(
        override val category: String = "Losses",
        override val displayName: String = "My Stats!",
        override val description: String = "Click here to view this achievement."
    ) : AchievementParent()

    // Obtain your first death.
    @Serializable
    @SerialName("FirstDeathParent")
    data class FirstDeathParent(
        override val category: String = "Deaths",
        override val displayName: String = "Oof!",
        override val description: String = "Click here to view this achievement."
    ) : AchievementParent()

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
}

@Serializable
data class Agent(
    val type: AgentType,
    @Contextual
    @Serializable
    val params: AgentParams? = null
)

@Serializable
data class Achievement(
    val _id: String,
    val name: String,
    val description: String,
    @Serializable
    val parent: AchievementParent? = null,
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