package http.player

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import network.warzone.api.database.Database
import network.warzone.api.database.model.Player
import network.warzone.api.database.model.Rank
import network.warzone.api.database.model.Session
import network.warzone.api.http.*
import network.warzone.api.http.player.*
import network.warzone.api.util.validate
import org.litote.kmongo.*
import java.security.MessageDigest
import java.util.*

fun Route.playerSessions() {
    post("/login") {
        validate<PlayerLoginRequest>(this) { data ->
            val now = System.currentTimeMillis()
            val ip = hashSHA256(data.ip)
            val activeSession =
                Session(playerId = data.playerId, createdAt = now, endedAt = null, _id = UUID.randomUUID().toString())

            val returningPlayer = Player.findById(data.playerId)

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
            val player = Player.findById(data.playerId) ?: throw PlayerMissingException()
            val activeSession = player.getActiveSession() ?: throw SessionInactiveException()

            activeSession.endedAt = System.currentTimeMillis()
            player.playtime += data.playtime

            Database.sessions.save(activeSession)
            Database.players.save(player)

            call.respond(Unit)
        }
    }
}

fun Route.playerRanks() {
    post("/{id}/ranks") {
        val id = call.parameters["id"] ?: throw ValidationException()
        val player = Player.findByIdOrName(id) ?: throw PlayerMissingException()

        validate<PlayerRanksModifyRequest>(this) { data ->
            val rank = Rank.findByIdOrName(data.rankId) ?: throw RankMissingException()

            if (rank._id in player.rankIds) throw RankAlreadyPresentException()
            player.rankIds = player.rankIds + rank._id

            Database.players.save(player)
            call.respond(PlayerProfileResponse(player))
        }
    }

    delete("/{id}/ranks") {
        val id = call.parameters["id"] ?: throw ValidationException()
        val player = Player.findByIdOrName(id) ?: throw PlayerMissingException()

        validate<PlayerRanksModifyRequest>(this) { data ->
            val rank = Rank.findByIdOrName(data.rankId) ?: throw RankMissingException()

            if (rank._id !in player.rankIds) throw RankNotPresentException()
            player.rankIds = player.rankIds.filterNot { it == rank._id }

            Database.players.save(player)
            call.respond(PlayerProfileResponse(player))
        }
    }
}


fun Application.playerRoutes() {
    routing {
        route("/mc/players") {
            playerSessions()
            playerRanks()
        }
    }
}

fun hashSHA256(ip: String) =
    MessageDigest.getInstance("SHA-256").digest(ip.toByteArray()).fold("", { str, it -> str + "%02x".format(it) })