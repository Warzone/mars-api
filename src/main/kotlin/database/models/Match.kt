package network.warzone.api.database.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import network.warzone.api.database.realtime.MatchEvent
import network.warzone.api.socket.SocketEvent
import network.warzone.api.socket.listeners.*

@Serializable
data class Match(
    val _id: String,
    val loadedAt: Long,
    var startedAt: Long?,
    var endedAt: Long?,
    val mapId: String,
//    val parties: List<Party>,
    val events: List<MatchEvent>,
    // player list?
)

//@Serializable
//data class MatchPlayer(val name: String, val uuid: String)
//
//@Serializable
//data class Party(
//    val name: String,
//    val alias: String,
//    val colour: String,
//    val min: Int,
//    val max: Int,
//    val members: Set<MatchPlayer>
//)