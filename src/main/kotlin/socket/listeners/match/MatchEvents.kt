package network.warzone.api.socket.listeners.match

import PartyData
import kotlinx.serialization.Serializable
import network.warzone.api.database.models.SimpleParticipant
import network.warzone.api.socket.event.ServerEvent
import network.warzone.api.socket.listeners.chat.ChatChannel
import network.warzone.api.socket.listeners.objective.GoalCollection
import network.warzone.api.socket.listeners.server.LiveGameServer
import java.util.*

open class MatchEvent(val match: LiveMatch) : ServerEvent(match.server)

class MatchLoadEvent(
    server: LiveGameServer, val data: MatchLoadData
) : ServerEvent(server) {
    @Serializable
    data class MatchLoadData(val mapId: String, val parties: List<PartyData>, val goals: GoalCollection)
}

class MatchStartEvent(match: LiveMatch, val data: MatchStartData) : MatchEvent(match) {
    @Serializable
    data class MatchStartData(val participants: Set<SimpleParticipant>)
}

class MatchEndEvent(match: LiveMatch, val data: MatchEndData) : MatchEvent(match) {
    @Serializable
    data class MatchEndData(val winningParty: String?, val bigStats: Map<String, BigStats>)

    @Serializable
    data class BigStats(
        var blocks: PlayerBlocksData?,
        var messages: Map<ChatChannel, Int>?,
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