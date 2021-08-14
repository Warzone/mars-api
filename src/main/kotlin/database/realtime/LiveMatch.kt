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
    val events: MutableList<MatchEvent>,
    val serverId: String,
    val participants: HashMap<String, LiveMatchPlayer>
) {
    private val server: LiveMinecraftServer
        get() {
            return ConnectionStore.minecraftServers.find { serverId == it.id }
                ?: throw RuntimeException("Cannot fetch server associated with match")
        }

    fun save() {
        Redis.set("match:$_id", this, SetParams().px(MATCH_LIFETIME))
    }
}

@Serializable
sealed class MatchEvent(val kind: SocketEvent, val time: Long)

@Serializable
@SerialName("MATCH_LOAD")
data class MatchLoadEvent(val data: MatchLoadData) : MatchEvent(SocketEvent.MATCH_LOAD, System.currentTimeMillis())

@Serializable
@SerialName("MATCH_START")
data class MatchStartEvent(val data: MatchStartData) : MatchEvent(SocketEvent.MATCH_START, System.currentTimeMillis())

@Serializable
@SerialName("PARTY_JOIN")
data class PartyJoinEvent(val data: PartyJoinData) : MatchEvent(SocketEvent.PARTY_JOIN, System.currentTimeMillis())

@Serializable
@SerialName("PARTY_LEAVE")
data class PartyLeaveEvent(val data: PartyLeaveData) : MatchEvent(SocketEvent.PARTY_LEAVE, System.currentTimeMillis())

@Serializable
@SerialName("PLAYER_DEATH")
data class PlayerDeathEvent(val data: PlayerDeathData) : MatchEvent(SocketEvent.PLAYER_DEATH, System.currentTimeMillis())