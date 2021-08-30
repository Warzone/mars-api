package network.warzone.api.socket2.listeners.party

import kotlinx.serialization.Serializable
import network.warzone.api.socket2.event.EventPriority
import network.warzone.api.socket2.event.FireAt
import network.warzone.api.socket2.event.Listener
import network.warzone.api.socket2.listeners.match.LiveMatch
import network.warzone.api.socket2.listeners.match.MatchEvent
import network.warzone.api.socket2.listeners.player.LiveMatchPlayer

class PartyListener : Listener() {
    override val handlers = mapOf(
        ::onJoin to PartyJoinEvent::class,
        ::onLeave to PartyLeaveEvent::class
    )

    // todo: ffa
    @FireAt(EventPriority.EARLY)
    suspend fun onJoin(event: PartyJoinEvent) {
        val match = event.match
//        val party = match.parties[event.data.partyName]
        val participant = match.participants[event.data.playerId]
        if (participant == null) { // Player is new to the match
            match.participants[event.data.playerId] = LiveMatchPlayer(event.data.playerId, event.data.playerName, event.data.partyName)
        } else { // Player has participated at some point (or is currently)
            participant.partyName = event.data.partyName
            match.participants[participant.id] = participant
        }
        match.save()
    }

    @FireAt(EventPriority.EARLY)
    suspend fun onLeave(event: PartyLeaveEvent) {
        val match = event.match
        val participant = match.participants[event.data.playerId]
        if (participant == null) {
            event.cancelled = true
            return println("Non-participant is leaving party? ${event.data.playerName}")
        }
        participant.partyName = null
        match.participants[participant.id] = participant
        match.save()
    }
}

class PartyJoinEvent(match: LiveMatch, val data: PartyJoinData) : MatchEvent(match) {
    @Serializable
    data class PartyJoinData(val playerId: String, val playerName: String, val partyName: String)
}

class PartyLeaveEvent(match: LiveMatch, val data: PartyLeaveData) : MatchEvent(match) {
    @Serializable
    data class PartyLeaveData(val playerId: String, val playerName: String)
}