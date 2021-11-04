package network.warzone.api.socket.listeners.match

import kotlinx.serialization.Serializable
import network.warzone.api.database.models.Match
import network.warzone.api.database.models.PlayerMessages
import network.warzone.api.database.models.SimpleParticipant
import network.warzone.api.socket.event.ServerEvent
import network.warzone.api.socket.listeners.objective.GoalCollection
import network.warzone.api.socket.listeners.server.LiveGameServer

open class MatchEvent(val match: Match) : ServerEvent(match.server)

class MatchLoadEvent(
    server: LiveGameServer, val data: MatchLoadData
) : ServerEvent(server) {
    @Serializable
    data class MatchLoadData(val mapId: String, val parties: List<PartyData>, val goals: GoalCollection)

    @Serializable
    data class PartyData(val name: String, val alias: String, val colour: String, val min: Int, val max: Int)
}

class MatchStartEvent(match: Match, val data: MatchStartData) : MatchEvent(match) {
    @Serializable
    data class MatchStartData(val participants: Set<SimpleParticipant>)
}

class MatchEndEvent(match: Match, val data: MatchEndData) : MatchEvent(match) {
    @Serializable
    data class MatchEndData(val winningParties: List<String>, val bigStats: Map<String, BigStats>)

    @Serializable
    data class BigStats(
        var blocks: PlayerBlocksData?,
        var messages: PlayerMessages,
        var bowShotsTaken: Int = 0,
        var bowShotsHit: Int = 0,
        var damageGiven: Double = 0.0,
        var damageTaken: Double = 0.0,
        var damageGivenBow: Double = 0.0
    ) {
        @Serializable
        data class PlayerBlocksData(var blocksPlaced: Map<String, Int> = mutableMapOf(), var blocksBroken: Map<String, Int> = mutableMapOf())
    }
}