package http.player

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import network.warzone.api.database.Database
import network.warzone.api.database.model.Player
import network.warzone.api.database.model.Rank
import network.warzone.api.database.model.Session
import network.warzone.api.http.PlayerMissingException
import network.warzone.api.http.SessionInactiveException
import network.warzone.api.http.player.PlayerLoginRequest
import network.warzone.api.http.player.PlayerLoginResponse
import network.warzone.api.http.player.PlayerLogoutRequest
import network.warzone.api.util.validate
import org.litote.kmongo.*
import java.security.MessageDigest
import java.util.*

fun Route.loginPlayer() {
    post("/login") {
        validate<PlayerLoginRequest>(this) { data ->
            val now = System.currentTimeMillis()
            val ip = hashSHA256(data.ip)
            val activeSession =
                Session(playerId = data.playerId, createdAt = now, endedAt = null, _id = UUID.randomUUID().toString())

            val returningPlayer = Database.players.findOne(Player::_id eq data.playerId)

            // todo: ensure username uniqueness

            // Player has joined before
            if (returningPlayer !== null) {
                // Delete any active sessions the player may have. Sessions should always be ended when the player leaves.
                Database.sessions.deleteMany(Session::endedAt eq null, Session::playerId eq data.playerId)

                returningPlayer.lastJoinedAt = now
                returningPlayer.name = data.playerName
                returningPlayer.nameLower = returningPlayer.name.toLowerCase()
                returningPlayer.ips =
                    if (ip in returningPlayer.ips) returningPlayer.ips else returningPlayer.ips + ip

                val ranksWithDefault = returningPlayer.rankIds + Rank.findDefault().map { it._id }
                returningPlayer.rankIds = ranksWithDefault.distinct()

                Database.players.save(returningPlayer)
                Database.sessions.save(activeSession)

                return@post call.respond(PlayerLoginResponse(player = returningPlayer, activeSession))
            } else { // Player is new!
                val name = data.playerName
                val player = Player(
                    _id = data.playerId,
                    name,
                    nameLower = name.toLowerCase(),
                    ips = listOf(ip),
                    firstJoinedAt = now,
                    lastJoinedAt = now,
                    playtime = 0,
                    rankIds = Rank.findDefault().map { it._id }
                )

                Database.players.insertOne(player)
                Database.sessions.save(activeSession)

                return@post call.respond(PlayerLoginResponse(player = player, activeSession))
            }

        }
    }

    post("/logout") {
        validate<PlayerLogoutRequest>(this) { data ->
            val player = Database.players.findOne(Player::_id eq data.playerId) ?: throw PlayerMissingException()
            val activeSession = player.getActiveSession() ?: throw SessionInactiveException()

            activeSession.endedAt = System.currentTimeMillis()
            player.playtime += data.playtime

            Database.sessions.save(activeSession)
            Database.players.save(player)

            call.respond(Unit)
        }
    }
}


fun Application.playerRoutes() {
    routing {
        route("/mc/players") {
            loginPlayer()
        }
    }
}

fun hashSHA256(ip: String) =
    MessageDigest.getInstance("SHA-256").digest(ip.toByteArray()).fold("", { str, it -> str + "%02x".format(it) })