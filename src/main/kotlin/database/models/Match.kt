package network.warzone.api.database.models

import kotlinx.serialization.Serializable
import network.warzone.api.socket.server.ConnectedServers
import network.warzone.api.socket.server.ServerContext
import java.util.*

@Serializable
data class Match(
    val _id: String,
    val loadedAt: Long,
    var startedAt: Long? = null,
    var endedAt: Long? = null,
    val level: Level,
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

    val server: ServerContext
        get() {
            return ConnectedServers.find { serverId == it.id }!!
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
    val color: String,
    val min: Int,
    val max: Int,
)

@Serializable
data class GoalCollection(
    val cores: List<CoreGoal>,
    val destroyables: List<DestroyableGoal>,
    val flags: List<FlagGoal>,
    val wools: List<WoolGoal>,
    val controlPoints: List<ControlPointGoal>
)

@Serializable
data class CoreGoal(
    val id: String,
    val name: String,
    val ownerName: String,
    val material: String,
    val contributors: Set<SimplePlayer> = emptySet()
)

@Serializable
data class DestroyableGoal(
    val id: String,
    val name: String,
    val ownerName: String,
    val material: String,
    val blockCount: Int,
    val breaksRequired: Int,
    val contributors: Set<SimplePlayer> = emptySet()
)

@Serializable
data class FlagGoal(val id: String, val name: String, val ownerName: String?, val color: String)

@Serializable
data class WoolGoal(val id: String, val name: String, val ownerName: String, val color: String)

@Serializable
data class ControlPointGoal(val id: String, val name: String)