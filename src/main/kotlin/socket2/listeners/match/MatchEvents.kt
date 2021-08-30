package network.warzone.api.socket2.listeners.match

import kotlinx.serialization.Serializable
import network.warzone.api.socket.listeners.PartyData
import network.warzone.api.socket2.event.ServerEvent
import network.warzone.api.socket2.listeners.objective.GoalCollection
import network.warzone.api.socket2.listeners.player.LiveMatchPlayer
import network.warzone.api.socket2.listeners.server.LiveGameServer

open class MatchEvent(val match: LiveMatch) : ServerEvent(match.server)

class MatchLoadEvent(
    server: LiveGameServer, val data: MatchLoadData
) : ServerEvent(server) {
    @Serializable
    data class MatchLoadData(val mapId: String, val parties: List<PartyData>, val goals: GoalCollection)
}

class MatchStartEvent(match: LiveMatch, val data: MatchStartData) : MatchEvent(match) {
    @Serializable
    data class MatchStartData(val participants: Set<LiveMatchPlayer>)
}

class MatchEndEvent(match: LiveMatch, val data: MatchEndData) : MatchEvent(match) {
    @Serializable
    data class MatchEndData(val winnerName: String?)
}