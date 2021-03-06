package network.warzone.api.http.leaderboard

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import network.warzone.api.http.UnauthorizedException
import network.warzone.api.http.ValidationException
import network.warzone.api.socket.leaderboard.LeaderboardPeriod
import network.warzone.api.socket.leaderboard.ScoreType

val publicScoreTypes = listOf(
    ScoreType.KILLS,
    ScoreType.DEATHS,
    ScoreType.FIRST_BLOODS,
    ScoreType.WINS,
    ScoreType.LOSSES,
    ScoreType.TIES,
    ScoreType.XP,
    ScoreType.CORE_LEAKS,
    ScoreType.CORE_BLOCK_DESTROYS,
    ScoreType.DESTROYABLE_DESTROYS,
    ScoreType.DESTROYABLE_BLOCK_DESTROYS,
    ScoreType.FLAG_CAPTURES,
    ScoreType.FLAG_DROPS,
    ScoreType.FLAG_PICKUPS,
    ScoreType.FLAG_DEFENDS,
    ScoreType.FLAG_HOLD_TIME,
    ScoreType.WOOL_CAPTURES,
    ScoreType.WOOL_DROPS,
    ScoreType.WOOL_PICKUPS,
    ScoreType.WOOL_DEFENDS,
    ScoreType.CONTROL_POINT_CAPTURES,
    ScoreType.HIGHEST_KILLSTREAK
)

fun Route.leaderboards() {
    get("/{score_type}/{period}") {
        val rawScoreType = call.parameters["score_type"] ?: throw ValidationException()
        val scoreType = ScoreType.find(rawScoreType) ?: throw ValidationException()
        if (!publicScoreTypes.contains(scoreType)) throw UnauthorizedException() // limit available leaderboards

        val rawPeriod = call.parameters["period"] ?: throw ValidationException()
        val period = LeaderboardPeriod.find(rawPeriod) ?: throw ValidationException()

        val limit = call.request.queryParameters["limit"]?.toInt() ?: 10

        val leaderboard = scoreType.toLeaderboard().fetchTop(period, if (limit > 50) 50 else limit)
        call.respond(leaderboard)
    }
}

fun Application.leaderboardRoutes() {
    routing {
        route("/mc/leaderboards") {
            leaderboards()
        }
    }
}