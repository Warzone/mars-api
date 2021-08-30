package network.warzone.api.database.realtime

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import network.warzone.api.database.Redis
import network.warzone.api.socket.ConnectionStore
import network.warzone.api.socket.SocketEvent
import network.warzone.api.socket.listeners.*
import redis.clients.jedis.params.SetParams

// Cached match expires after one day
const val MATCH_LIFETIME = 86400000L // MS

@Serializable
data class LiveMatch(
    val _id: String,
    val loadedAt: Long,
    var startedAt: Long?,
    var endedAt: Long?,
    val mapId: String,
    var parties: List<LiveParty>,
    val goals: GoalCollection,
    val events: MutableList<MatchEvent>,
    val serverId: String,
    val participants: HashMap<String, LiveMatchPlayer>
) {
    val server: LiveMinecraftServer
        get() {
            return ConnectionStore.minecraftServers.find { serverId == it.id }
                ?: throw RuntimeException("Cannot fetch server associated with match")
        }

    fun save() {
        Redis.set("match:$_id", this, SetParams().px(MATCH_LIFETIME))
    }
}

@Serializable
open class MatchEvent(val kind: SocketEvent, val time: Long)

@Serializable
data class GoalCollection(
    val cores: List<CoreGoal>,
    val destroyables: List<DestroyableGoal>,
    val flags: List<FlagGoal>,
    val wools: List<WoolGoal>,
    val controlPoints: List<ControlPointGoal>
)

@Serializable
data class CoreGoal(val id: String, val name: String, val ownerName: String, val material: String)

@Serializable
data class DestroyableGoal(
    val id: String,
    val name: String,
    val ownerName: String,
    val material: String,
    val blockCount: Int
)

@Serializable
data class FlagGoal(val id: String, val name: String, val ownerName: String?, val colour: String)

@Serializable
data class WoolGoal(val id: String, val name: String, val ownerName: String, val colour: String)

@Serializable
data class ControlPointGoal(val id: String, val name: String)

@Serializable
@SerialName("PARTY_JOIN")
data class PartyMemberAddEvent(val data: PartyMemberAddData) : MatchEvent(SocketEvent.PARTY_JOIN, System.currentTimeMillis())

@Serializable
@SerialName("PARTY_LEAVE")
data class PartyMemberRemoveEvent(val data: PartyMemberRemoveData) : MatchEvent(SocketEvent.PARTY_LEAVE, System.currentTimeMillis())
