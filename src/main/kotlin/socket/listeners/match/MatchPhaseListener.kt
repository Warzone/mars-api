package network.warzone.api.socket.listeners.match

import network.warzone.api.database.Database
import network.warzone.api.database.MatchCache
import network.warzone.api.database.models.Match
import network.warzone.api.database.models.MatchState
import network.warzone.api.database.models.Participant
import network.warzone.api.database.models.Party
import network.warzone.api.socket.event.EventPriority
import network.warzone.api.socket.event.FireAt
import network.warzone.api.socket.event.Listener
import java.util.*

class MatchPhaseListener : Listener() {
    override val handlers = mapOf(
        ::onLoad to MatchLoadEvent::class,
        ::onStart to MatchStartEvent::class,
        ::onEnd to MatchEndEvent::class
    )

    @FireAt(EventPriority.EARLY)
    suspend fun onLoad(event: MatchLoadEvent) {
        val now = Date().time
        val parties = hashMapOf<String, Party>()
        val level = Database.levels.findOneById(event.data.mapId) ?: return // todo: force state change
        val match = Match(
            _id = UUID.randomUUID().toString(),
            loadedAt = now,
            startedAt = null,
            endedAt = null,
            level = level,
            parties = parties,
            participants = hashMapOf(),
            serverId = event.server.id,
            goals = event.data.goals,
            firstBlood = null
        )

        event.data.parties.forEach {
            parties[it.name] = Party(

                name = it.name,
                alias = it.alias,
                colour = it.colour,
                min = it.min,
                max = it.max
            )
        }
        match.parties = parties

        MatchCache.set(match._id, match)

        event.server.currentMatchId = match._id
    }

    @FireAt(EventPriority.EARLY)
    suspend fun onStart(event: MatchStartEvent) {
        val match = event.match
        if (match.state != MatchState.PRE) {
            event.cancelled = true
            return println("Cannot start match ${match._id} on invalid state (${match.state})")
        }
        val participants = event.data.participants.map { Participant(it) }.toTypedArray()
        match.startedAt = Date().time
        match.saveParticipants(*participants)
        MatchCache.set(match._id, match)
    }

    @FireAt(EventPriority.EARLIEST)
    suspend fun onEnd(event: MatchEndEvent) {
        val match = event.match
        if (match.state != MatchState.IN_PROGRESS) {
            event.cancelled = true
            return println("Cannot end match ${match._id} on invalid state (${match.state})")
        }
        match.endedAt = Date().time
        MatchCache.set(match._id, match)
    }
}
