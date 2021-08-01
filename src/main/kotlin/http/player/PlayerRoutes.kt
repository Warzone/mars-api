package http.player

import io.ktor.application.*
import io.ktor.routing.*
import kotlinx.coroutines.runBlocking
import network.warzone.api.http.player.PlayerLoginData
import network.warzone.api.util.validate
import java.security.MessageDigest

fun Route.loginPlayer() = runBlocking {
    post("/login") {
        validate<PlayerLoginData>(this) { data ->
            val ip = hashIP(data.ip)
        }
    }
}

fun Application.playerRoutes() {
    routing {
        route("/api/mc/players") {
            loginPlayer()
        }
    }
}

fun hashIP(ip: String) =
    MessageDigest.getInstance("SHA-256").digest(ip.toByteArray()).fold("", { str, it -> str + "%02x".format(it) })