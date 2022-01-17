package network.warzone.api.socket.match

import network.warzone.api.database.Database
import network.warzone.api.database.MatchCache
import network.warzone.api.database.models.Match
import network.warzone.api.database.models.MatchState
import network.warzone.api.database.models.Participant
import network.warzone.api.database.models.Party
import network.warzone.api.socket.server.ServerContext
import network.warzone.api.socket.server.MatchLoadData
import java.util.*

class MatchPhaseListener(val server: ServerContext) {
    suspend fun onLoad(data: MatchLoadData) {
        val level = Database.levels.findOneById(data.mapId) ?: return // todo: force cycle

        val now = Date().time

        val match = Match(
            _id = UUID.randomUUID().toString(),
            loadedAt = now,
            startedAt = null,
            endedAt = null,
            level = level,
            parties = hashMapOf(),
            participants = hashMapOf(),
            serverId = server.id,
            goals = data.goals,
            firstBlood = null
        )

        val parties = hashMapOf<String, Party>()
        data.parties.forEach {
            parties[it.name] = Party(
                name = it.name,
                alias = it.alias,
                color = it.color,
                min = it.min,
                max = it.max
            )
        }
        match.parties = parties

        MatchCache.set(match._id, match, true)
        server.currentMatchId = match._id
    }

    suspend fun onStart(data: MatchStartData, match: Match): Match? {
        if (match.state != MatchState.PRE) return null // todo: force cycle

        match.startedAt = Date().time

        val participants = data.participants.map { Participant(it) }.toTypedArray()
        match.saveParticipants(*participants)

        return match
    }

    suspend fun onEnd(data: MatchEndData, match: Match): Match? {
        if (match.state != MatchState.IN_PROGRESS) return null // todo: force cycle
        match.endedAt = Date().time
        return match
    }
}