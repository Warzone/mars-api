package network.warzone.api

import http.player.playerRoutes
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import io.ktor.websocket.*
import network.warzone.api.http.ApiException
import network.warzone.api.http.ApiExceptionResponse
import network.warzone.api.http.map.mapRoutes
import network.warzone.api.http.rank.rankRoutes
import network.warzone.api.http.tag.tagRoutes
import network.warzone.api.socket2.initSocketHandler2

fun main() {
    embeddedServer(Netty, host = "0.0.0.0", port = 3000) {
        Server().apply { main() }
    }.start(wait = true)
}

class Server {
    fun Application.main() {
        install(ContentNegotiation) {
            json()
        }

        install(StatusPages) {
            exception<ApiException> { ex ->
                call.respond(ex.statusCode, ApiExceptionResponse(ex.type.code, ex.message))
            }

            exception<Throwable> { cause ->
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiExceptionResponse(
                        "INTERNAL_SERVER_ERROR",
                        "An internal server error occurred. This should not happen."
                    )
                )
                log.error(cause)
            }

        }

        install(WebSockets)

        initSocketHandler2()

        playerRoutes()
        rankRoutes()
        tagRoutes()
        mapRoutes()
    }
}