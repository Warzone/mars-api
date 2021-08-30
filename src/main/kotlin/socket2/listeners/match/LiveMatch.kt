package network.warzone.api.socket2.listeners.match

import kotlinx.serialization.Serializable
import network.warzone.api.database.Redis
import network.warzone.api.database.realtime.MATCH_LIFETIME
import network.warzone.api.socket2.listeners.party.LiveParty
import network.warzone.api.socket2.listeners.objective.GoalCollection
import network.warzone.api.socket2.listeners.player.LiveMatchPlayer
import network.warzone.api.socket2.listeners.server.ConnectedServers
import network.warzone.api.socket2.listeners.server.LiveGameServer
import redis.clients.jedis.params.SetParams

// Cached match expires after one day
const val MATCH_LIFETIME = 86400000L // MS

/*
* Represents a realtime match
*/
@Serializable
data class LiveMatch(
    val id: String,
    val loadedAt: Long,
    var startedAt: Long?,
    var endedAt: Long?,
    val mapId: String,
    val goals: GoalCollection,
    var parties: HashMap<String, LiveParty>,
    val participants: HashMap<String, LiveMatchPlayer>,
    val serverId: String,
) {
    val server: LiveGameServer
        get() {
            return ConnectedServers.find { serverId == it.id }
                ?: throw RuntimeException("Cannot fetch server associated with match")
        }

    fun save() {
        Redis.set("match:$id", this, SetParams().px(MATCH_LIFETIME))
    }

    val state: MatchState
    get() {
        return if (this.startedAt == null) MatchState.PRE
        else if (this.endedAt == null) MatchState.IN_PROGRESS
        else MatchState.POST
    }
}

enum class MatchState {
    PRE,
    IN_PROGRESS,
    POST
}