package network.warzone.api.http.status

import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.text.get

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