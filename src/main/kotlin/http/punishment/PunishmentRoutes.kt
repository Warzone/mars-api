package network.warzone.api.http.punishment

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import network.warzone.api.Config
import network.warzone.api.database.Database
import network.warzone.api.database.findById
import network.warzone.api.database.models.PunishmentReversion
import network.warzone.api.http.PunishmentMissingException
import network.warzone.api.http.ValidationException
import network.warzone.api.util.protected
import network.warzone.api.util.validate
import java.util.*

fun Route.managePunishments() {
    get("/types") {
        protected(this) { _ ->
            call.respond(Config.punishmentTypes)
        }
    }

    get("/{punishmentId}") {
        protected(this) { _ ->
            val id = call.parameters["punishmentId"] ?: throw ValidationException()
            val punishment = Database.punishments.findById(id) ?: throw PunishmentMissingException()
            call.respond(punishment)
        }
    }

    post("/{punishmentId}/revert") {
        protected(this) { _ ->
            val id = call.parameters["punishmentId"] ?: throw ValidationException()
            validate<PunishmentRevertRequest>(this) { data ->
                val punishment = Database.punishments.findById(id) ?: throw PunishmentMissingException()
                punishment.reversion = PunishmentReversion(Date().time, data.reverter, data.reason)
                Database.punishments.save(punishment)
                call.respond(punishment)
            }
        }
    }
}

fun Application.punishmentRoutes() {
    routing {
        route("/mc/punishments") {
            managePunishments()
        }
    }
}