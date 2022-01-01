package network.warzone.api.socket.participant

import kotlinx.serialization.Serializable
import network.warzone.api.database.models.Match
import network.warzone.api.database.models.Participant
import network.warzone.api.socket.match.MatchEndData
import network.warzone.api.socket.player.PlayerContext

data class ParticipantContext(val profile: Participant, val match: Match) {
    fun getMatchResult(end: MatchEndData): PlayerMatchResult {
        val isPlaying = profile.partyName != null
        if (!isPlaying) return PlayerMatchResult.INDETERMINATE

        return when {
            end.isTie(match) -> PlayerMatchResult.TIE
            !end.isTie(match) && end.winningParties.contains(profile.partyName) -> PlayerMatchResult.WIN
            !end.winningParties.contains(profile.partyName) -> PlayerMatchResult.LOSE
            else -> PlayerMatchResult.INDETERMINATE
        }
    }

    suspend fun getPlayerContext(): PlayerContext {
        return PlayerContext(profile.getPlayer()!!, match)
    }
}

@Serializable
enum class PlayerMatchResult {
    WIN,
    LOSE,
    TIE,
    INDETERMINATE
}