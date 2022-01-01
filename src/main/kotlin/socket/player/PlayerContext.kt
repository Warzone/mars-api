package network.warzone.api.socket.player

import network.warzone.api.database.models.Match
import network.warzone.api.database.models.Player
import network.warzone.api.socket.EventType
import network.warzone.api.socket.participant.ParticipantContext

data class PlayerContext(val profile: Player, val match: Match) {
    fun getParticipant(): ParticipantContext {
        val profile = match.participants[profile._id]!!
        return ParticipantContext(profile, match)
    }

    suspend fun sendMessage(message: String, sound: String? = null) {
        match.server.call(EventType.MESSAGE, MessageData(message, sound, listOf(profile._id)))
    }
}