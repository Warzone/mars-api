package network.warzone.api.socket.listeners.objective

import kotlinx.serialization.Serializable

@Serializable
data class GoalContribution(val playerId: String, val percentage: Float, val blockCount: Int)

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
data class FlagPickupDefendData(val flagId: String, val playerId: String)

@Serializable
data class FlagPlaceDropData(val flagId: String, val playerId: String, val heldTime: Long)

@Serializable
data class WoolEventData(val woolId: String, val playerId: String)