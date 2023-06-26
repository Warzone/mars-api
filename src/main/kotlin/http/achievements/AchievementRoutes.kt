package network.warzone.api.http.achievements

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import network.warzone.api.database.models.Achievement
import java.util.*

fun Route.addAchievement() {
    post {
        val newAchievementRequest = call.receive<AchievementCreateRequest>()
        val newAchievement = Achievement(
            id = UUID.randomUUID().toString(),
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

fun Route.deleteAchievement() {
    delete("/{id}") {
        val id = call.parameters["id"]
        if (id == null) {
            call.respond(HttpStatusCode.BadRequest, "Missing or malformed id")
            return@delete
        }

        val isDeleted = Achievement.deleteAchievement(id)
        if (isDeleted) {
            call.respond(HttpStatusCode.OK, "Achievement $id deleted successfully")
        } else {
            call.respond(HttpStatusCode.NotFound, "Achievement $id not found")
        }
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
            deleteAchievement()
        }
    }
}