package http.player

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import network.warzone.api.Config
import network.warzone.api.database.Database
import network.warzone.api.database.PlayerCache
import network.warzone.api.database.findById
import network.warzone.api.database.findByIdOrName
import network.warzone.api.database.models.*
import network.warzone.api.http.*
import network.warzone.api.http.player.*
import network.warzone.api.http.punishment.PunishmentIssueRequest
import network.warzone.api.socket.EventType
import network.warzone.api.socket.leaderboard.ServerPlaytimeLeaderboard
import network.warzone.api.socket.player.DisconnectPlayerData
import network.warzone.api.socket.server.ConnectedServers
import network.warzone.api.util.WebhookUtil
import network.warzone.api.util.protected
import network.warzone.api.util.validate
import org.litote.kmongo.contains
import org.litote.kmongo.div
import org.litote.kmongo.eq
import java.security.MessageDigest
import java.util.*

fun Route.playerSessions() {
    post("/{playerId}/prelogin") {
        protected(this) { _ ->
            validate<PlayerPreLoginRequest>(this) { data ->
                if (call.parameters["playerId"] != data.player.id) throw ValidationException("Player ID in URL does not match body")

                val now = Date().time
                val ip = hashIp(data.ip)

                val returningPlayer = Database.players.findById(data.player.id)

                // Player has joined before
                if (returningPlayer != null) {
                    val activeSession = returningPlayer.getActiveSession()
                    if (activeSession != null) {
                        // Disconnect player from server they're already on
                        val server = ConnectedServers.find { it.id == activeSession.serverId }
                        server?.call(
                            EventType.DISCONNECT_PLAYER,
                            DisconnectPlayerData(returningPlayer._id, "You logged into another server.")
                        )
                    }

                    returningPlayer.name = data.player.name
                    returningPlayer.nameLower = returningPlayer.name.lowercase()
                    returningPlayer.ips =
                        if (ip in returningPlayer.ips) returningPlayer.ips else returningPlayer.ips + ip

                    val playerPunishments = returningPlayer.getActivePunishments()
                    val playerBan = playerPunishments.firstOrNull { it.action.isBan }
                    val ipPunishments = if (playerBan != null) Database.punishments.find(
                        Punishment::targetIps contains ip,
                        Punishment::action / PunishmentAction::kind eq PunishmentKind.IP_BAN
                    ).toList() else emptyList()
                    val ipBan = ipPunishments.firstOrNull()

                    val banned = playerBan != null || ipBan != null

                    PlayerCache.set(returningPlayer.name, returningPlayer, persist = true)

                    call.respond(
                        PlayerPreLoginResponse(
                            new = false,
                            allowed = !banned,
                            player = returningPlayer,
                            playerPunishments + ipPunishments
                        )
                    )

                    Player.ensureNameUniqueness(data.player.name, data.player.id)
                } else { // Player is new!
                    val player = Player(
                        _id = data.player.id,
                        name = data.player.name,
                        nameLower = data.player.name.lowercase(),
                        ips = listOf(ip),
                        firstJoinedAt = now.toDouble(),
                        lastJoinedAt = now.toDouble(),
                        rankIds = emptyList(),
                        tagIds = emptyList(),
                        activeTagId = null,
                        stats = PlayerStats(),
                        gamemodeStats = hashMapOf(),
                        notes = emptyList(),
                        lastSessionId = null
                    )

                    PlayerCache.set(player.name, player, persist = true)

                    call.respond(
                        HttpStatusCode.Created,
                        PlayerPreLoginResponse(new = true, allowed = true, player, emptyList())
                    )

                    Player.ensureNameUniqueness(data.player.name, data.player.id)
                }
            }
        }
    }

    post("/{playerId}/login") {
        protected(this) { serverId ->
            validate<PlayerPreLoginRequest>(this) { data ->
                val playerId = call.parameters["playerId"] ?: throw ValidationException()
                val player: Player = PlayerCache.get(data.player.name) ?: throw PlayerMissingException()
                if (playerId != player._id || playerId != data.player.id) throw ValidationException()

                val now = Date().time
                val ip = hashIp(data.ip)

                val activeSession =
                    Session(
                        _id = UUID.randomUUID().toString(),
                        player = player.simple,
                        ip = ip,
                        serverId = serverId!!,
                        createdAt = now,
                        endedAt = null,
                    )

                Database.sessions.save(activeSession)

                val defaultRanks = player.rankIds + Rank.findDefault().map { it._id }
                player.rankIds = defaultRanks.distinct()

                player.lastJoinedAt = now.toDouble()
                player.lastSessionId = activeSession._id

                PlayerCache.set(player.name, player, persist = true)

                call.respond(HttpStatusCode.Created, PlayerLoginResponse(activeSession))
            }
        }
    }

    post("/logout") {
        protected(this) { _ ->
            validate<PlayerLogoutRequest>(this) { data ->
                val player: Player = PlayerCache.get(data.player.name) ?: throw PlayerMissingException()
                val activeSession = player.getActiveSession() ?: throw SessionInactiveException()

                activeSession.endedAt = Date().time
                player.stats.serverPlaytime += data.playtime

                ServerPlaytimeLeaderboard.increment(player.idName, data.playtime.toInt()) // Will break in 2038

                // Longest Session Record
                val recordSession = player.stats.records.longestSession?.length
                if (recordSession == null || data.playtime > recordSession) player.stats.records.longestSession =
                    SessionRecord(activeSession._id, data.playtime)

                Database.sessions.save(activeSession)
                PlayerCache.set(player.name, player, persist = true)

                call.respond(Unit)
            }
        }
    }

    get("/{playerId}") {
        val playerId = call.parameters["playerId"]?.lowercase() ?: throw ValidationException()
        val player: Player = PlayerCache.get(playerId) ?: throw PlayerMissingException()

        call.respond(player.sanitise())
    }
}

fun Route.playerModeration() {
    post("/{playerId}/punishments") {
        protected(this) { serverId ->
            validate<PunishmentIssueRequest>(this) { data ->
                val id = UUID.randomUUID().toString()
                val now = Date().time
                val target: Player = PlayerCache.get(data.targetName) ?: throw PlayerMissingException()
                val punishment = Punishment(
                    _id = id,
                    reason = data.reason,
                    issuedAt = now.toDouble(),
                    offence = data.offence,
                    action = data.action,
                    note = data.note,
                    punisher = data.punisher,
                    target = target.simple,
                    targetIps = data.targetIps,
                    silent = data.silent,
                    serverId = serverId
                )
                Database.punishments.insertOne(punishment)
                call.respond(punishment)
                WebhookUtil.sendPunishmentWebhook(punishment)
            }
        }
    }

    get("/{playerId}/punishments") {
        protected(this) { _ ->
            val playerId = call.parameters["playerId"] ?: throw ValidationException()
            val player = PlayerCache.get<Player>(playerId) ?: throw PlayerMissingException()
            call.respond(player.getPunishments())
        }
    }

    get("/{playerId}/lookup") {
        protected(this) { _ ->
            val playerId = call.parameters["playerId"] ?: throw ValidationException()
            val includeAlts = call.request.queryParameters["alts"] == "true"
            val player = PlayerCache.get<Player>(playerId) ?: throw PlayerMissingException()
            val alts = mutableListOf<PlayerAltResponse>()
            if (includeAlts) player.getAlts().forEach {
                val puns = it.getPunishments()
                alts.add(PlayerAltResponse(it, puns))
            }
            call.respond(PlayerLookupResponse(player, alts))
        }
    }

    post("/{playerId}/notes") {
        protected(this) { _ ->
            val playerId = call.parameters["playerId"] ?: throw ValidationException()
            val player = PlayerCache.get<Player>(playerId) ?: throw PlayerMissingException()
            validate<PlayerAddNoteRequest>(this) { data ->
                val id = (player.notes.maxByOrNull { it.id }?.id ?: 0) + 1
                val note = StaffNote(id, data.author, data.content, Date().time)
                player.notes += note
                PlayerCache.set(playerId, player, true)
                call.respond(player)
            }
        }
    }

    delete("/{playerId}/notes/{noteId}") {
        protected(this) { _ ->
            val playerId = call.parameters["playerId"] ?: throw ValidationException()
            val noteId = call.parameters["noteId"]?.toInt() ?: throw ValidationException()
            val player = PlayerCache.get<Player>(playerId) ?: throw PlayerMissingException()
            val note = player.notes.find { it.id == noteId } ?: throw NoteMissingException()
            player.notes -= note
            PlayerCache.set(playerId, player, true)
            call.respond(player)
        }
    }
}

fun Route.playerTags() {
    put("/{playerId}/active_tag") {
        protected(this) { _ ->
            validate<PlayerSetActiveTagRequest>(this) { data ->
                val playerId = call.parameters["playerId"] ?: throw ValidationException()
                val tagId = data.activeTagId

                val player: Player = PlayerCache.get(playerId) ?: throw PlayerMissingException()

                if (tagId == player.activeTagId) return@protected call.respond(player)

                if (tagId == null) {
                    player.activeTagId = null
                } else {
                    if (tagId !in player.tagIds) throw TagNotPresentException()
                    player.activeTagId = tagId
                }

                PlayerCache.set(player.name, player, persist = true)
                call.respond(player)
            }
        }
    }

    put("/{playerId}/tags/{tagId}") {
        protected(this) { _ ->
            val playerId = call.parameters["playerId"] ?: throw ValidationException()
            val tagId = call.parameters["tagId"] ?: throw ValidationException()

            val player: Player = PlayerCache.get(playerId) ?: throw PlayerMissingException()
            val tag = Database.tags.findByIdOrName(tagId) ?: throw TagMissingException()

            if (tag._id in player.tagIds) throw TagAlreadyPresentException()
            player.tagIds = player.tagIds + tag._id

            PlayerCache.set(player.name, player, persist = true)
            call.respond(player)
        }
    }

    delete("/{playerId}/tags/{tagId}") {
        protected(this) { _ ->
            val playerId = call.parameters["playerId"] ?: throw ValidationException()
            val tagId = call.parameters["tagId"] ?: throw ValidationException()

            val player: Player = PlayerCache.get(playerId) ?: throw PlayerMissingException()
            val tag = Database.tags.findByIdOrName(tagId) ?: throw TagMissingException()

            if (tag._id !in player.tagIds) throw TagNotPresentException()
            player.tagIds = player.tagIds.filterNot { it == tag._id }
            if (player.activeTagId == tag._id) player.activeTagId = null

            PlayerCache.set(player.name, player, persist = true)
            call.respond(player)
        }
    }
}

fun Route.playerRanks() {
    put("/{playerId}/ranks/{rankId}") {
        protected(this) { _ ->
            val playerId = call.parameters["playerId"] ?: throw ValidationException()
            val rankId = call.parameters["rankId"] ?: throw ValidationException()

            val player: Player = PlayerCache.get(playerId) ?: throw PlayerMissingException()
            val rank = Database.ranks.findByIdOrName(rankId) ?: throw RankMissingException()

            if (rank._id in player.rankIds) throw RankAlreadyPresentException()
            player.rankIds = player.rankIds + rank._id

            PlayerCache.set(player.name, player, persist = true)
            call.respond(player)
        }
    }

    delete("/{playerId}/ranks/{rankId}") {
        protected(this) { _ ->
            val playerId = call.parameters["playerId"] ?: throw ValidationException()
            val rankId = call.parameters["rankId"] ?: throw ValidationException()

            val player: Player = PlayerCache.get(playerId) ?: throw PlayerMissingException()
            val rank = Database.ranks.findByIdOrName(rankId) ?: throw RankMissingException()

            if (rank._id !in player.rankIds) throw RankNotPresentException()
            player.rankIds = player.rankIds.filterNot { it == rank._id }

            PlayerCache.set(player.name, player, persist = true)
            call.respond(player)
        }
    }
}

fun Application.playerRoutes() {
    routing {
        route("/mc/players") {
            playerSessions()
            playerRanks()
            playerTags()
            playerModeration()
        }
    }
}

fun hashIp(ip: String): String = if (Config.enableIpHashing) hashSHA256(ip) else ip

fun hashSHA256(ip: String) =
    MessageDigest.getInstance("SHA-256").digest(ip.toByteArray()).fold("") { str, it -> str + "%02x".format(it) }