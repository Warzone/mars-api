package network.warzone.api.http.achievements

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import network.warzone.api.database.Database
import network.warzone.api.database.findByIdOrName
import network.warzone.api.database.models.Achievement
import network.warzone.api.http.AchievementMissingException
import network.warzone.api.http.RankMissingException
import network.warzone.api.http.ValidationException
import network.warzone.api.util.capitalizeFirst
import network.warzone.api.util.protected
import network.warzone.api.util.validate
import java.util.*

fun Route.addAchievement() {
    post {
        protected(this) { _ ->
            validate<AchievementCreateRequest>(this) { data ->
                val newAchievementRequest = data
                val newAchievement = Achievement(
                    _id = UUID.randomUUID().toString(),
                    name = newAchievementRequest.name,
                    description = newAchievementRequest.description,
                    parent = newAchievementRequest.parent,
                    agent = newAchievementRequest.agent
                )
                val resultId = Achievement.addAchievement(newAchievement)
                call.respond(HttpStatusCode.Created, resultId)
            }
        }
    }
}

fun Route.getAchievements() {
    get {
        val achievements = Achievement.getAchievements()
        call.respond(HttpStatusCode.OK, achievements)
    }
    get("/{achievementId}") {
        val id = call.parameters["achievementId"] ?: throw ValidationException()
        val achievement = Database.achievements.findByIdOrName(id) ?: throw AchievementMissingException()
        call.respond(achievement)
    }
}

fun Route.deleteAchievement() {
    delete("/{id}") {
        protected(this) { _ ->
            val id = call.parameters["id"]
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing or malformed id")
                return@protected
            }

            val isDeleted = Achievement.deleteAchievement(id)
            if (isDeleted) {
                call.respond(HttpStatusCode.OK, "Achievement $id deleted successfully")
            } else {
                call.respond(HttpStatusCode.NotFound, "Achievement $id not found")
            }
        }
    }
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