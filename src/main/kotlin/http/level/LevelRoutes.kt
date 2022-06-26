package network.warzone.api.http.level

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import network.warzone.api.Config

fun Route.levelColors() {
    get("colors") {
        call.respond(Config.levelColors)
    }
}

fun Application.levelRoutes() {
    routing {
        route("/mc/levels") {
            levelColors()
        }
    }
}