package network.warzone.api.http.achievements

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.addAchievement() {
    post {
        val newAchievement = call.receive<AchievementCreateRequest>()
        call.respond(HttpStatusCode.Created, "id of the created achievement")
    }
}

fun Route.getAchievements() {
    get {
        // Fetch the achievements from your database
        call.respond(listOf<AchievementCreateRequest>()) // replace with actual achievements
    }
}

// TODO: Do this one later; it needs a different route
fun Route.getCompletionStatus() {

}

fun Application.achievementRoutes() {
    routing {
        route("/mc/achievements") {
            //put something here
        }
    }
}