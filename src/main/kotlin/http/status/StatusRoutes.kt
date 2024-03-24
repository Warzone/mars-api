package network.warzone.api.http.status

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.status() {
    get() {
        // TODO: Different status codes
        call.respond(
            HttpStatusCode.OK,
            StatusResponse(
                HttpStatusCode.OK.description, // "OK"
            )
        )
    }
}

fun Application.statusRoutes() {
    routing {
        route("status") {
            status()
        }
    }
}