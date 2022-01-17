package network.warzone.api.socket.server

import kotlinx.serialization.Serializable
import network.warzone.api.database.models.GoalCollection

@Serializable
data class MatchLoadData(val mapId: String, val parties: List<PartyData>, val goals: GoalCollection) {
    @Serializable
    data class PartyData(val name: String, val alias: String, val color: String, val min: Int, val max: Int)
}