package network.warzone.api.socket2.listeners.player

import kotlinx.serialization.Serializable
import network.warzone.api.database.Redis
import network.warzone.api.socket2.listeners.match.LiveMatch

/*
* Represents a player participating in a match in realtime
* A LiveMatchPlayer may also exist for a player who once participated in the match but has since left, so their stats are saved at the end of the match
*/
@Serializable
data class LiveMatchPlayer(val id: String, val name: String, var partyName: String?, val matchId: String, var kills: Int, var deaths: Int) {
    fun save() {
        val match = Redis.get<LiveMatch>("match:$id") ?: throw RuntimeException("Match not found in Redis $matchId")
        match.participants[this.id] = this
        match.save()
    }
}