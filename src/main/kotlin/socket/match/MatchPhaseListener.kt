package network.warzone.api.socket.match

import network.warzone.api.database.Database
import network.warzone.api.database.MatchCache
import network.warzone.api.database.models.Match
import network.warzone.api.database.models.MatchState
import network.warzone.api.database.models.Participant
import network.warzone.api.database.models.Party
import network.warzone.api.socket.InvalidMatchStateException
import network.warzone.api.socket.logger
import network.warzone.api.socket.server.MatchLoadData
import network.warzone.api.socket.server.ServerContext
import java.util.*

class MatchPhaseListener(val server: ServerContext) {
    suspend fun onLoad(data: MatchLoadData) {
        val level = Database.levels.findOneById(data.mapId) ?: throw InvalidMatchStateException()

        val now = Date().time

        val matchId = UUID.randomUUID().toString()
        level.goals = data.goals
        level.lastMatchId = matchId

        val match = Match(
            _id = matchId,
            loadedAt = now,
            startedAt = null,
            endedAt = null,
            level = level,
            parties = hashMapOf(),
            participants = hashMapOf(),
            serverId = server.id,
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
        logger.debug("(${server.id}) Match loaded: ${match._id}")
    }

    suspend fun onStart(data: MatchStartData, match: Match): Match {
        if (match.state != MatchState.PRE) throw InvalidMatchStateException()

        match.startedAt = Date().time

        val participants = data.participants.map { Participant(it) }.toTypedArray()
        match.saveParticipants(*participants)

        logger.debug("(${server.id}) Match started: ${match._id}")

        return match
    }

    suspend fun onEnd(data: MatchEndData, match: Match): Match {
        if (match.state != MatchState.IN_PROGRESS) throw InvalidMatchStateException()
        match.endedAt = Date().time
        logger.debug("(${server.id}) Match ended: ${match._id}")
        return match
    }
}