package network.warzone.api.socket2.listeners.match

import network.warzone.api.socket2.event.*
import network.warzone.api.socket2.listeners.party.LiveParty
import java.util.*

class MatchListener : Listener() {
    override val handlers = mapOf(
        ::onLoad to MatchLoadEvent::class,
        ::onStart to MatchStartEvent::class,
        ::onEnd to MatchEndEvent::class
    )

    @FireAt(EventPriority.EARLY)
    suspend fun onLoad(event: MatchLoadEvent) {
        val now = System.currentTimeMillis()
        val parties = hashMapOf<String, LiveParty>()
        val match = LiveMatch(
            id = UUID.randomUUID().toString(),
            loadedAt = now,
            startedAt = null,
            endedAt = null,
            mapId = event.data.mapId,
            parties = parties,
            participants = hashMapOf(),
            serverId = event.server.id,
            goals = event.data.goals
        )

        event.data.parties.forEach {
            parties[it.name] = LiveParty(
                name = it.name,
                alias = it.alias,
                colour = it.colour,
                min = it.min,
                max = it.max
            )
        }
        match.parties = parties

        match.save()
        event.server.currentMatchId = match.id
    }

    @FireAt(EventPriority.EARLY)
    suspend fun onStart(event: MatchStartEvent) {
        val current = event.match
        if (current.state != MatchState.PRE) {
            event.cancelled = true
            return println("Cannot start match ${current.id} on invalid state (${current.state})")
        }
        current.startedAt = System.currentTimeMillis()
        event.data.participants.forEachIndexed { index, player ->
            current.participants[player.id] = event.data.participants.elementAt(index)
        }
        current.save()
    }

    @FireAt(EventPriority.EARLY)
    suspend fun onEnd(event: MatchEndEvent) {
        val current = event.match
        if (current.state != MatchState.IN_PROGRESS) {
            event.cancelled = true
            return println("Cannot end match ${current.id} on invalid state (${current.state})")
        }
        current.endedAt = System.currentTimeMillis()
        current.save()
    }
}
