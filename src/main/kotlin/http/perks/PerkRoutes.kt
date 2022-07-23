package network.warzone.api.http.perks

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import network.warzone.api.Config
import network.warzone.api.database.PlayerCache
import network.warzone.api.database.models.Player
import network.warzone.api.http.PlayerMissingException
import network.warzone.api.http.ValidationException
import network.warzone.api.util.protected
import network.warzone.api.util.validate

fun Route.joinSounds() {
    route("join_sounds") {
        get {
            call.respond(Config.joinSounds)
        }

        post("/{playerId}/sound") {
            protected(this) { _ ->
                validate<JoinSoundSetRequest>(this) { data ->
                    val playerId = call.parameters["playerId"] ?: throw ValidationException()
                    val player: Player = PlayerCache.get(playerId) ?: throw PlayerMissingException()

                    if (data.activeJoinSoundId == player.activeJoinSoundId)
                        return@protected call.respond(player)

                    player.activeJoinSoundId = data.activeJoinSoundId

                    PlayerCache.set(player.name, player, persist = true)
                    call.respond(player)
                }
            }
        }
    }
}

fun Application.perkRoutes() {
    routing {
        route("/mc/perks") {
            joinSounds()
        }
    }
}