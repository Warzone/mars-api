package network.warzone.api.http.server

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import network.warzone.api.database.Database
import network.warzone.api.database.MatchCache
import network.warzone.api.database.PlayerCache
import network.warzone.api.database.Redis
import network.warzone.api.database.models.Match
import network.warzone.api.database.models.Player
import network.warzone.api.database.models.ServerEvents
import network.warzone.api.database.models.Session
import network.warzone.api.http.UnauthorizedException
import network.warzone.api.http.ValidationException
import network.warzone.api.util.protected
import network.warzone.api.util.validate
import org.litote.kmongo.eq
import org.litote.kmongo.replaceOne
import redis.clients.jedis.params.SetParams
import java.util.*

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
                Redis.get<Long>("server:$serverId:last_alive_time")
            if (lastAliveTime == null) {
                // Set new alive time
                Redis.set("server:$serverId:last_alive_time", Date().time)

                return@protected call.respond(Unit)
            }

            val lastMatchId =
                Redis.get<String>("server:$serverId:current_match_id") ?: return@protected call.respond(Unit)
            val match = Redis.get<Match>("match:$lastMatchId")
            if (match != null) {
                match.endedAt = lastAliveTime
                MatchCache.set(match._id, match, true, SetParams().px(3600000L)) // expire cache after 1 hour
            }

            val hangingSessions =
                Database.sessions.find(Session::serverId eq serverId, Session::endedAt eq null).toList()

            val sessionsToWrite = mutableListOf<Session>()
            val playersToWrite = mutableListOf<Player>()

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

            // Set new alive time
            Redis.set("server:$serverId:last_alive_time", Date().time)

            application.log.info("Saved ${playersToWrite.size} players, ${sessionsToWrite.size} sessions on startup '$serverId'")
            call.respond(Unit)
        }
    }

    get("/{serverId}/status") {
        val serverId = call.parameters["serverId"]?.lowercase() ?: throw ValidationException("Server ID parameter is required")
        val lastAliveTime =
            Redis.get<Long>("server:$serverId:last_alive_time") ?: return@get call.respond(HttpStatusCode.NotFound, "Last alive time unknown")
        val currentMatchId = Redis.get<String>("server:$serverId:current_match_id") ?: return@get call.respond(HttpStatusCode.NotFound, "No current match")
        val match = Redis.get<Match>("match:$currentMatchId") ?: return@get call.respond(HttpStatusCode.NotFound, "No current match")
        call.respond(ServerStatusResponse(lastAliveTime, match, match.isTrackingStats))
    }

    get("/{serverId}/events") {
        val serverId = call.parameters["serverId"]?.lowercase() ?: throw ValidationException("Server ID parameter is required")
        val events = Redis.get<ServerEvents>("server:$serverId:events") ?: ServerEvents(null)
        call.respond(events)
    }

    put("/{serverId}/events/xp_multiplier") {
        protected(this) { serverId ->
            serverId ?: throw ValidationException()
            if (serverId != call.parameters["serverId"]) throw UnauthorizedException()

            validate<XPMultiplierRequest>(this) { data ->
                val events = Redis.get<ServerEvents>("server:$serverId:events") ?: ServerEvents(null)
                events.xpMultiplier = if (data.value == 1f) null else data.toXPMultiplier()
                Redis.set("server:$serverId:events", events)
                call.respond(events)
            }
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