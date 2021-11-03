package network.warzone.api.socket.listeners.party

import kotlinx.serialization.Serializable
import network.warzone.api.database.models.Participant
import network.warzone.api.database.models.SimpleParticipant
import network.warzone.api.socket.event.EventPriority
import network.warzone.api.socket.event.FireAt
import network.warzone.api.socket.event.Listener
import network.warzone.api.socket.listeners.match.LiveMatch
import network.warzone.api.socket.listeners.match.MatchEvent

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
            participant.joinedPartyAt = System.currentTimeMillis()

            val timeAway = System.currentTimeMillis() - participant.lastLeftPartyAt!!
            participant.stats.timeAway += timeAway

            match.participants[participant.id] = participant
        }
        match.save()
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
        participant.lastLeftPartyAt = System.currentTimeMillis()
        participant.stats.gamePlaytime += (System.currentTimeMillis() - participant.joinedPartyAt!!)
        participant.joinedPartyAt = null
        match.saveParticipants(participant)
    }
}

class PartyJoinEvent(match: LiveMatch, val data: PartyJoinData) : MatchEvent(match) {
    val simpleParticipant = SimpleParticipant(data.playerName, data.playerId, data.partyName)
    val participant = match.participants[data.playerId]

    @Serializable
    data class PartyJoinData(val playerId: String, val playerName: String, val partyName: String)
}

class PartyLeaveEvent(match: LiveMatch, val data: PartyLeaveData) : MatchEvent(match) {
    val participant = match.participants[data.playerId]

    @Serializable
    data class PartyLeaveData(val playerId: String, val playerName: String)
}