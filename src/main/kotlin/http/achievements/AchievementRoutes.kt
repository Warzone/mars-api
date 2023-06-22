package network.warzone.api.http.achievements

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import network.warzone.api.database.Database
import network.warzone.api.database.findById
import network.warzone.api.database.models.Achievement
import network.warzone.api.database.models.Player

fun Route.addAchievement() {
    post {
        val newAchievementRequest = call.receive<AchievementCreateRequest>()
        val newAchievement = Achievement(
            id = newAchievementRequest.id,
            name = newAchievementRequest.name,
            description = newAchievementRequest.description,
            agent = newAchievementRequest.agent
        )
        val resultId = Achievement.addAchievement(newAchievement)
        call.respond(HttpStatusCode.Created, resultId)
    }
}

fun Route.getAchievements() {
    get {
        val achievements = Achievement.getAchievements()
        call.respond(HttpStatusCode.OK, achievements)
    }
}

// TODO: Do this one later; it needs a different route
fun Route.getCompletionStatus() {

}

fun Application.achievementRoutes() {
    routing {
        route("/mc/achievements") {
            addAchievement()
            getAchievements()
        }
        routing {
            webSocket("/mc/achievements/completions/updates") {
                while (true) {
                    val frame = incoming.receive()
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        val event = Json.decodeFromString<AchievementCompletionEvent>(text)
                        handleAchievementCompletionEvent(event)
                    }
                }
            }
        }
    }
}

suspend fun DefaultWebSocketSession.handleAchievementCompletionEvent(event: AchievementCompletionEvent) {
    val achievement = Achievement.findById(event.achievementId)
    val player = Database.players.findById(event.playerId)
    if (achievement != null && player != null) {
        // Add the achievement to the player's list of completed achievements
        player.stats.achievements.add(achievement.id)
        // Update the player in the database
        // Player.update(player)
        // Send a message back to the client
        outgoing.send(Frame.Text("Achievement ${achievement.name} completed by player ${player.name}"))
    } else {
        outgoing.send(Frame.Text("Invalid player ID or achievement ID"))
    }
}