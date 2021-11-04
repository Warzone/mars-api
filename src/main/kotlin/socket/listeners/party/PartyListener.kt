package network.warzone.api.socket.listeners.party

import kotlinx.serialization.Serializable
import network.warzone.api.database.MatchCache
import network.warzone.api.database.models.Match
import network.warzone.api.database.models.Participant
import network.warzone.api.database.models.SimpleParticipant
import network.warzone.api.socket.event.EventPriority
import network.warzone.api.socket.event.FireAt
import network.warzone.api.socket.event.Listener
import network.warzone.api.socket.listeners.match.MatchEvent
import java.util.*

class PartyListener : Listener() {
    override val handlers = mapOf(
        ::onJoin to PartyJoinEvent::class,
        ::onLeave to PartyLeaveEvent::class
    )

    // todo: ffa
    @FireAt(EventPriority.EARLY)
    suspend fun onJoin(event: PartyJoinEvent) {
        val match = event.match
        val participant = event.participant
        if (participant == null) { // Player is new to the match
            match.participants[event.data.playerId] = Participant(event.simpleParticipant)
        } else { // Player has participated at some point (or is currently)
            participant.partyName = event.data.partyName
            participant.lastPartyName = event.data.partyName
            participant.joinedPartyAt = Date().time

            val timeAway = Date().time - participant.lastLeftPartyAt!!
            participant.stats.timeAway += timeAway

            match.participants[participant.id] = participant
        }
        MatchCache.set(match._id, match)
    }

    @FireAt(EventPriority.EARLY)
    suspend fun onLeave(event: PartyLeaveEvent) {
        val match = event.match
        val participant = event.participant
        if (participant == null) {
            event.cancelled = true
            return println("Non-participant is leaving party? ${event.data.playerName}")
        }
        participant.partyName = null
        participant.lastLeftPartyAt = Date().time
        participant.stats.gamePlaytime += (Date().time - participant.joinedPartyAt!!)
        participant.joinedPartyAt = null
        match.saveParticipants(participant)
        MatchCache.set(match._id, match)
    }
}

class PartyJoinEvent(match: Match, val data: PartyJoinData) : MatchEvent(match) {
    val simpleParticipant = SimpleParticipant(data.playerName, data.playerId, data.partyName)
    val participant = match.participants[data.playerId]

    @Serializable
    data class PartyJoinData(val playerId: String, val playerName: String, val partyName: String)
}

class PartyLeaveEvent(match: Match, val data: PartyLeaveData) : MatchEvent(match) {
    val participant = match.participants[data.playerId]

    @Serializable
    data class PartyLeaveData(val playerId: String, val playerName: String)
}