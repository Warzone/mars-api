package network.warzone.api.socket.listeners.objective

import kotlinx.serialization.Serializable
import network.warzone.api.database.models.SimplePlayer

@Serializable
data class GoalCollection(
    val cores: List<CoreGoal>,
    val destroyables: List<DestroyableGoal>,
    val flags: List<FlagGoal>,
    val wools: List<WoolGoal>,
    val controlPoints: List<ControlPointGoal>
)

@Serializable
data class CoreGoal(val id: String, val name: String, val ownerName: String, val material: String)

@Serializable
data class DestroyableGoal(
    val id: String,
    val name: String,
    val ownerName: String,
    val material: String,
    val blockCount: Int
)

@Serializable
data class FlagGoal(val id: String, val name: String, val ownerName: String?, val colour: String)

@Serializable
data class WoolGoal(val id: String, val name: String, val ownerName: String, val colour: String)

@Serializable
data class ControlPointGoal(val id: String, val name: String)