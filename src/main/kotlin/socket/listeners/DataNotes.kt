import kotlinx.serialization.Serializable
import network.warzone.api.database.models.SimpleParticipant
import network.warzone.api.socket.listeners.objective.GoalCollection

@Serializable
data class ControlPointCaptureData(val pointId: String, val playerIds: List<String>, val partyName: String)

@Serializable
data class CoreLeakData(val coreId: String, val contributions: Set<GoalContribution>)

@Serializable
data class CoreDamageData(val coreId: String, val playerId: String)

@Serializable
data class DestroyableDestroyData(val destroyableId: String, val contributions: Set<GoalContribution>)

@Serializable
data class DestroyableDamageData(val destroyableId: String, val playerId: String)

@Serializable
data class GoalContribution(val playerId: String, val percentage: Float, val blockCount: Int)

@Serializable
data class FlagEventData(val flagId: String, val playerId: String)

@Serializable
data class FlagHeldData(val flagId: String, val playerId: String, val heldTime: Long)

@Serializable
data class WoolEventData(val woolId: String, val playerId: String)

@Serializable
data class MatchLoadData(val mapId: String, val parties: List<PartyData>, val goals: GoalCollection)

@Serializable
data class PartyData(val name: String, val alias: String, val colour: String, val min: Int, val max: Int)

@Serializable
data class MatchStartData(val participants: Set<SimpleParticipant>)

@Serializable
enum class DamageCause {
    MELEE,
    PROJECTILE,
    EXPLOSION,
    FIRE,
    LAVA,
    POTION,
    FLATTEN,
    FALL,
    PRICK,
    DROWN,
    STARVE,
    SUFFOCATE,
    SHOCK,
    VOID,
    UNKNOWN
}

@Serializable
data class PlayerDeathData(
    val victimId: String,
    val victimName: String,
    val attackerId: String? = null,
    val attackerName: String? = null,
    val weapon: String? = null,
    val entity: String? = null,
    val distance: Int? = null,
    val key: String,
    val cause: DamageCause
)

@Serializable
data class InboundPlayerChatData(
    val channel: ChatChannel,
    val playerName: String,
    val playerId: String,
    val message: String,
    val playerPrefix: String
)

@Serializable
data class OutboundPlayerChatData(
    val channel: ChatChannel,
    val playerName: String,
    val playerId: String,
    val message: String,
    val serverId: String,
    val playerPrefix: String
)

@Serializable
enum class ChatChannel {
    STAFF,
    GLOBAL,
    TEAM
}

