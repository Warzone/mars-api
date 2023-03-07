package network.warzone.api.http.match

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import network.warzone.api.database.MatchCache
import network.warzone.api.database.models.Match
import network.warzone.api.http.MatchMissingException
import network.warzone.api.http.ValidationException

fun Route.matches() {
    get("/{matchId}") {
        val matchId = call.parameters["matchId"]?.lowercase() ?: throw ValidationException()
        val match: Match = MatchCache.get(matchId) ?: throw MatchMissingException()
        call.respond(match)
    }
}

fun Application.matchRoutes() {
    routing {
        route("/mc/matches") {
            matches()
        }
    }
}