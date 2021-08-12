package http.player

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import network.warzone.api.database.Database
import network.warzone.api.database.findById
import network.warzone.api.database.findByIdOrName
import network.warzone.api.database.models.Player
import network.warzone.api.database.models.Rank
import network.warzone.api.database.models.Session
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

            val returningPlayer = Database.players.findById(data.playerId)

            // todo: ensure username uniqueness

            // Player has joined before
            if (returningPlayer !== null) {
                // todo: account for multi-server. kick player from server if they're joining a diff server.
                // Delete any active sessions the player may have. Sessions should always be ended when the player leaves.
                Database.sessions.deleteMany(Session::endedAt eq null, Session::playerId eq data.playerId)

                returningPlayer.lastJoinedAt = now
                returningPlayer.name = data.playerName
                returningPlayer.nameLower = returningPlayer.name.lowercase()
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
                    nameLower = name.lowercase(),
                    ips = listOf(ip),
                    firstJoinedAt = now,
                    lastJoinedAt = now,
                    playtime = 0,
                    rankIds = Rank.findDefault().map { it._id },
                    tagIds = emptyList(),
                    activeTagId = null
                )

                Database.players.insertOne(player)
                Database.sessions.save(activeSession)

                return@post call.respond(HttpStatusCode.Created, PlayerLoginResponse(player, activeSession))
            }

        }
    }

    post("/logout") {
        validate<PlayerLogoutRequest>(this) { data ->
            val player = Database.players.findById(data.playerId) ?: throw PlayerMissingException()
            val activeSession = player.getActiveSession() ?: throw SessionInactiveException()

            activeSession.endedAt = System.currentTimeMillis()
            player.playtime += data.playtime

            Database.sessions.save(activeSession)
            Database.players.save(player)

            call.respond(Unit)
        }
    }

    get("/{playerId}") {
        val playerId = call.parameters["playerId"] ?: throw ValidationException()
        val player = Database.players.findByIdOrName(playerId) ?: throw PlayerMissingException()

        call.respond(player)
    }
}

fun Route.playerTags() {
    put("/{playerId}/active_tag") {
        validate<PlayerSetActiveTagRequest>(this) { data ->
            val playerId = call.parameters["playerId"] ?: throw ValidationException()
            val tagId = data.activeTagId

            val player = Database.players.findByIdOrName(playerId) ?: throw PlayerMissingException()

            if (tagId == player.activeTagId) return@put call.respond(player)

            if (tagId == null) {
                player.activeTagId = null
            } else {
                if (tagId !in player.tagIds) throw TagNotPresentException()
                player.activeTagId = tagId
            }

            Database.players.save(player)
            call.respond(player)
        }
    }

    put("/{playerId}/tags/{tagId}") {
        println("request ${call.parameters.toMap()}")

        val playerId = call.parameters["playerId"] ?: throw ValidationException()
        val tagId = call.parameters["tagId"] ?: throw ValidationException()

        val player = Database.players.findById(playerId) ?: throw PlayerMissingException()
        val tag = Database.tags.findByIdOrName(tagId) ?: throw TagMissingException()

        if (tag._id in player.tagIds) throw TagAlreadyPresentException()
        player.tagIds = player.tagIds + tag._id

        Database.players.save(player)
        call.respond(player)

        println(player)
    }

    delete("/{playerId}/tags/{tagId}") {
        val playerId = call.parameters["playerId"] ?: throw ValidationException()
        val tagId = call.parameters["tagId"] ?: throw ValidationException()

        val player = Database.players.findById(playerId) ?: throw PlayerMissingException()
        val tag = Database.tags.findByIdOrName(tagId) ?: throw TagMissingException()

        if (tag._id !in player.tagIds) throw TagNotPresentException()
        player.tagIds = player.tagIds.filterNot { it == tag._id }
        if (player.activeTagId == tag._id) player.activeTagId = null

        Database.players.save(player)
        call.respond(player)
    }
}

fun Route.playerRanks() {
    put("/{playerId}/ranks/{rankId}") {
        val playerId = call.parameters["playerId"] ?: throw ValidationException()
        val rankId = call.parameters["rankId"] ?: throw ValidationException()

        val player = Database.players.findById(playerId) ?: throw PlayerMissingException()
        val rank = Database.ranks.findById(rankId) ?: throw RankMissingException()

        if (rank._id in player.rankIds) throw RankAlreadyPresentException()
        player.rankIds = player.rankIds + rank._id

        Database.players.save(player)
        call.respond(player)
    }

    delete("/{playerId}/ranks/{rankId}") {
        val playerId = call.parameters["playerId"] ?: throw ValidationException()
        val rankId = call.parameters["rankId"] ?: throw ValidationException()

        val player = Database.players.findById(playerId) ?: throw PlayerMissingException()
        val rank = Database.ranks.findById(rankId) ?: throw RankMissingException()

        if (rank._id !in player.rankIds) throw RankNotPresentException()
        player.rankIds = player.rankIds.filterNot { it == rank._id }

        Database.players.save(player)
        call.respond(player)
    }
}

fun Application.playerRoutes() {
    routing {
        route("/mc/players") {
            playerSessions()
            playerRanks()
            playerTags()
        }
    }
}

fun hashSHA256(ip: String) =
    MessageDigest.getInstance("SHA-256").digest(ip.toByteArray()).fold("") { str, it -> str + "%02x".format(it) }