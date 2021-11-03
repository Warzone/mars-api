package network.warzone.api.socket.listeners.match

import kotlinx.serialization.Serializable
import network.warzone.api.database.Redis
import network.warzone.api.database.models.FirstBlood
import network.warzone.api.database.models.Participant
import network.warzone.api.socket.listeners.party.LiveParty
import network.warzone.api.socket.listeners.objective.GoalCollection
import network.warzone.api.socket.listeners.server.ConnectedServers
import network.warzone.api.socket.listeners.server.LiveGameServer
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
    var parties: Map<String, LiveParty>,
    val participants: HashMap<String, Participant>,
    val serverId: String,
    var firstBlood: FirstBlood?
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

    fun saveParticipants(vararg participants: Participant) {
        participants.forEach {
            this.participants[it.id] = it
        }
        this.save()
    }
}

enum class MatchState {
    PRE,
    IN_PROGRESS,
    POST
}