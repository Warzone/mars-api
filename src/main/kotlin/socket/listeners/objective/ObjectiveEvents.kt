package network.warzone.api.socket.listeners.objective

import kotlinx.serialization.Serializable
import network.warzone.api.socket.listeners.match.LiveMatch
import network.warzone.api.socket.listeners.match.MatchEvent

@Serializable
data class GoalContribution(val playerId: String, val percentage: Float, val blockCount: Int)

class ControlPointCaptureEvent(match: LiveMatch, val data: ControlPointCaptureData) : MatchEvent(match) {
    @Serializable
    data class ControlPointCaptureData(val pointId: String, val playerIds: List<String>, val partyName: String)
}

class CoreLeakEvent(match: LiveMatch, val data: CoreLeakData) : MatchEvent(match) {
    @Serializable
    data class CoreLeakData(val coreId: String, val contributions: Set<GoalContribution>)
}

class CoreDamageEvent(match: LiveMatch, val data: CoreDamageData) : MatchEvent(match) {
    @Serializable
    data class CoreDamageData(val coreId: String, val playerId: String)
}

class DestroyableDestroyEvent(match: LiveMatch, val data: DestroyableDestroyData) : MatchEvent(match) {
    @Serializable
    data class DestroyableDestroyData(val destroyableId: String, val contributions: Set<GoalContribution>)
}

class DestroyableDamageEvent(match: LiveMatch, val data: DestroyableDamageData) : MatchEvent(match) {
    @Serializable
    data class DestroyableDamageData(val destroyableId: String, val playerId: String)
}

class FlagPickupEvent(match: LiveMatch, val data: FlagPickupData) : MatchEvent(match) {
    val participant = match.participants[data.playerId]!!

    @Serializable
    data class FlagPickupData(val flagId: String, val playerId: String)
}

class FlagDefendEvent(match: LiveMatch, val data: FlagPickupEvent.FlagPickupData) : MatchEvent(match) {
    val participant = match.participants[data.playerId]!!
}

class FlagDropEvent(match: LiveMatch, val data: FlagDropData) : MatchEvent(match) {
    val participant = match.participants[data.playerId]!!

    @Serializable
    data class FlagDropData(val flagId: String, val playerId: String, val heldTime: Long)
}

class FlagPlaceEvent(match: LiveMatch, val data: FlagDropEvent.FlagDropData) : MatchEvent(match) {
    val participant = match.participants[data.playerId]!!
}

class WoolPickupEvent(match: LiveMatch, val data: WoolPickupData) : MatchEvent(match) {
    val participant = match.participants[data.playerId]!!

    @Serializable
    data class WoolPickupData(val woolId: String, val playerId: String)
}

class WoolDefendEvent(match: LiveMatch, val data: WoolPickupEvent.WoolPickupData) : MatchEvent(match) {
    val participant = match.participants[data.playerId]!!
}

class WoolDropEvent(match: LiveMatch, val data: WoolPickupEvent.WoolPickupData) : MatchEvent(match) {
    val participant = match.participants[data.playerId]!!
}

class WoolPlaceEvent(match: LiveMatch, val data: WoolPickupEvent.WoolPickupData) : MatchEvent(match) {
    val participant = match.participants[data.playerId]!!
}

