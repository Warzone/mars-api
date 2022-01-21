package network.warzone.api.http.server

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import network.warzone.api.database.Database
import network.warzone.api.database.MatchCache
import network.warzone.api.database.PlayerCache
import network.warzone.api.database.Redis
import network.warzone.api.database.models.Match
import network.warzone.api.database.models.Player
import network.warzone.api.database.models.Session
import network.warzone.api.http.InternalServerErrorException
import network.warzone.api.http.UnauthorizedException
import network.warzone.api.http.ValidationException
import network.warzone.api.util.protected
import org.litote.kmongo.eq
import org.litote.kmongo.replaceOne
import redis.clients.jedis.params.SetParams

fun Route.manageServers() {
    /**
     * This is called when a Mars server starts, before the WebSocket connection is opened.
     *
     * Used to save/resolve any pending stats, matches or sessions (if the server previously crashed)
     */
    post("/{serverId}/startup") {
        protected(this) { serverId ->
            serverId ?: throw ValidationException()
            if (serverId != call.parameters["serverId"]) throw UnauthorizedException()

            val lastAliveTime =
                Redis.get<Long>("server:$serverId:last_alive_time") ?: return@protected call.respond(Unit)

            val lastMatchId =
                Redis.get<String>("server:$serverId:current_match_id") ?: return@protected call.respond(Unit)
            val match = Redis.get<Match>("match:$lastMatchId") ?: throw InternalServerErrorException()

            val hangingSessions =
                Database.sessions.find(Session::serverId eq serverId, Session::endedAt eq null).toList()

            val sessionsToWrite = mutableListOf<Session>()
            val playersToWrite = mutableListOf<Player>()

            match.endedAt = lastAliveTime
            MatchCache.set(match._id, match, true, SetParams().px(3600000L)) // expire cache after 1 hour

            hangingSessions.forEach {
                it.endedAt = lastAliveTime
                sessionsToWrite.add(it)

                val cachedPlayer = PlayerCache.get<Player>(it.player.name) ?: return@forEach
                cachedPlayer.stats.serverPlaytime += it.length!!
                playersToWrite.add(cachedPlayer)
            }

            if (playersToWrite.isNotEmpty()) Database.players.bulkWrite(playersToWrite.map {
                replaceOne(
                    Player::_id eq it._id,
                    it
                )
            })
            if (sessionsToWrite.isNotEmpty()) Database.sessions.bulkWrite(sessionsToWrite.map {
                replaceOne(
                    Session::_id eq it._id,
                    it
                )
            })

            println("Saved ${playersToWrite.size} players, ${sessionsToWrite.size} sessions on startup '$serverId'")
            call.respond(Unit)
        }
    }
}

fun Application.serverRoutes() {
    routing {
        route("/mc/servers") {
            manageServers()
        }
    }
}