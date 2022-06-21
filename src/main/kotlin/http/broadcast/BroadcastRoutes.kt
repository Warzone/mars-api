package network.warzone.api.http.broadcast

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
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