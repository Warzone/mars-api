package network.warzone.api.http.broadcast

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import network.warzone.api.Config

fun Route.manageBroadcasts() {
    get {
        call.respond(Config.broadcasts)
    }
}

fun Application.broadcastRoutes() {
    routing {
        route("/mc/broadcasts") {
            manageBroadcasts()
        }
    }
}