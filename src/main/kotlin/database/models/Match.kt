package network.warzone.api.database.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import network.warzone.api.socket.SocketEvent
import network.warzone.api.socket.listeners.MatchLoadData
import network.warzone.api.socket.listeners.MatchStartData
import network.warzone.api.socket.listeners.PartyJoinData
import network.warzone.api.socket.listeners.PartyLeaveData

@Serializable
data class Match(
    val _id: String,
    val loadedAt: Long,
    var startedAt: Long?,
    var endedAt: Long?,
    val mapId: String,
    val parties: List<Party>,
    val events: List<MatchEvent>,
    // player list?
)

@Serializable
data class MatchPlayer(val name: String, val uuid: String)

@Serializable
data class Party(
    val name: String,
    val alias: String,
    val colour: String,
    val min: Int,
    val max: Int,
    val members: Set<MatchPlayer>
)

/* Match Events (Base + Specific Events) */
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