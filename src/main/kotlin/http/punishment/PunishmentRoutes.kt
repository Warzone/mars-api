package network.warzone.api.http.punishment

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import network.warzone.api.Config
import network.warzone.api.database.Database
import network.warzone.api.database.findById
import network.warzone.api.http.PunishmentMissingException
import network.warzone.api.http.ValidationException

fun Route.managePunishments() {
    get("/types") {
        call.respond(Config.punishmentTypes)
    }

    get("/{punishmentId}") {
        val id = call.parameters["punishmentId"] ?: throw ValidationException()
        val punishment = Database.punishments.findById(id) ?: throw PunishmentMissingException()
        call.respond(punishment)
    }
}

fun Application.punishmentRoutes() {
    routing {
        route("/mc/punishments") {
            managePunishments()
        }
    }
}