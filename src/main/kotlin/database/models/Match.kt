package network.warzone.api.database.models

import kotlinx.serialization.Serializable
import network.warzone.api.socket.listeners.objective.GoalCollection
import network.warzone.api.socket.listeners.server.ConnectedServers
import network.warzone.api.socket.listeners.server.LiveGameServer
import java.util.*

@Serializable
data class Match(
    val _id: String,
    val loadedAt: Long,
    var startedAt: Long?,
    var endedAt: Long?,
    val level: Level,
    val goals: GoalCollection,
    var parties: Map<String, Party>,
    val participants: HashMap<String, Participant>,
    val serverId: String,
    var firstBlood: FirstBlood?
) {
    val state: MatchState
        get() {
            return if (this.startedAt == null) MatchState.PRE
            else if (this.endedAt == null) MatchState.IN_PROGRESS
            else MatchState.POST
        }

    val length: Long
        get() {
            val start = startedAt ?: 0
            val end = endedAt ?: Date().time
            return end - start
        }

    val server: LiveGameServer
        get() {
            return ConnectedServers.find { serverId == it.id }
                ?: throw RuntimeException("Cannot fetch server associated with match")
        }

    fun saveParticipants(vararg participants: Participant): Match {
        participants.forEach {
            this.participants[it.id] = it
        }
        return this
    }
}

enum class MatchState {
    PRE,
    IN_PROGRESS,
    POST
}

@Serializable
data class FirstBlood(var attacker: SimplePlayer, var victim: SimplePlayer, var date: Long)

@Serializable
data class Party(
    val name: String,
    val alias: String,
    val colour: String,
    val min: Int,
    val max: Int,
)
