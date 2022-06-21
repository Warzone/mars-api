package network.warzone.api.http.level

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
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