package network.warzone.api

import http.player.playerRoutes
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import network.warzone.api.http.ApiException
import network.warzone.api.http.ApiExceptionResponse
import network.warzone.api.socket.initSocketHandler

fun main() {
    embeddedServer(Netty, host = "0.0.0.0", port = 3000) {
        install(ContentNegotiation) {
            json()
        }

        install(StatusPages) {
            exception<ApiException> { ex ->
                call.respond(ex.statusCode, ApiExceptionResponse(ex.type.code, ex.message))
            }
            exception<Throwable> { cause ->
                call.respond(HttpStatusCode.InternalServerError, "Internal Server Error")
                throw cause
            }
        }

        install(WebSockets)

        initSocketHandler()

        playerRoutes()
    }.start(wait = true)
}