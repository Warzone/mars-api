package network.warzone.api.database.realtime

import kotlinx.serialization.Serializable
import network.warzone.api.database.Redis
import network.warzone.api.database.models.MatchEvent
import network.warzone.api.socket.ConnectionStore

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
        Redis.set("match:$_id", this)
    }
}