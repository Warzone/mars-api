package network.warzone.api.socket.listeners

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import network.warzone.api.database.realtime.*
import network.warzone.api.socket.SocketEvent
import network.warzone.api.socket.SocketListener
import network.warzone.api.socket.format
import java.util.*

object MatchListener : SocketListener() {
    override suspend fun handle(server: LiveMinecraftServer, event: SocketEvent, json: JsonObject) {
        when (event) {
            SocketEvent.MATCH_LOAD -> onLoad(server, format.decodeFromJsonElement(json))
            SocketEvent.MATCH_START -> onStart(server, format.decodeFromJsonElement(json))
            SocketEvent.MATCH_END -> onEnd(server)
            else -> Unit
        }
    }

    private fun onLoad(server: LiveMinecraftServer, data: MatchLoadData) {
        val now = System.currentTimeMillis()
        val match = LiveMatch(
            _id = UUID.randomUUID().toString(),
            loadedAt = now,
            startedAt = null,
            endedAt = null,
            mapId = data.mapId,
            events = mutableListOf(MatchLoadEvent(data)),
            parties = data.parties.map {
                LiveParty(
                    it.name,
                    it.alias,
                    it.colour,
                    it.min,
                    it.max
                )
            },
            participants = hashMapOf(),
            serverId = server.id,
            goals = data.goals
        )

        match.save()
        server.currentMatchId = match._id
    }

    private fun onStart(server: LiveMinecraftServer, data: MatchStartData) {
        val current = server.currentMatch ?: return println("Can't find current match")
        if (current.startedAt !== null) return println("Starting match that already started?")
        current.startedAt = System.currentTimeMillis()
        data.participants.forEachIndexed { index, player ->
            current.participants[player.id] = data.participants.elementAt(index)
        }
        current.events.add(MatchStartEvent(data))
        current.save()
    }

    // todo: process "match metadata" — known participants, team mappings,
    // todo: save to DB and all --- this is unfinished
    private fun onEnd(server: LiveMinecraftServer) {
        val current = server.currentMatch ?: return println("Ending non-existent match? ${server.id}")
        if (current.startedAt == null) return println("Trying to end match before it's started")
        if (current.endedAt != null) return println("Trying to end match that's already ended somehow")
        current.endedAt = System.currentTimeMillis()
        current.save()
    }
}

@Serializable
data class MatchLoadData(val mapId: String, val parties: List<PartyData>, val goals: GoalCollection)

@Serializable
data class PartyData(val name: String, val alias: String, val colour: String, val min: Int, val max: Int)

@Serializable
@SerialName("MATCH_LOAD")
data class MatchLoadEvent(val data: MatchLoadData) : MatchEvent(SocketEvent.MATCH_LOAD, System.currentTimeMillis())

@Serializable
data class MatchStartData(val participants: Set<LiveMatchPlayer>)

@Serializable
@SerialName("MATCH_START")
data class MatchStartEvent(val data: MatchStartData) : MatchEvent(SocketEvent.MATCH_START, System.currentTimeMillis())

@Serializable
data class GoalContribution(val playerId: String, val percentage: Float, val blockCount: Int)

@Serializable
enum class DamageCause {
    MELEE,
    PROJECTILE,
    EXPLOSION,
    FIRE,
    LAVA,
    POTION,
    FLATTEN,
    FALL,
    PRICK,
    DROWN,
    STARVE,
    SUFFOCATE,
    SHOCK,
    VOID,
    UNKNOWN
}