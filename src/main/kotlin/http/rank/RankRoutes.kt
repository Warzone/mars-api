package network.warzone.api.http.rank

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import network.warzone.api.database.*
import network.warzone.api.database.models.Player
import network.warzone.api.database.models.Rank
import network.warzone.api.http.RankConflictException
import network.warzone.api.http.RankMissingException
import network.warzone.api.http.ValidationException
import network.warzone.api.util.validate
import org.litote.kmongo.contains
import org.litote.kmongo.eq
import org.litote.kmongo.not
import java.util.*

fun Route.manageRanks() {
    post {
        validate<RankCreateRequest>(this) { data ->
            val conflict = Database.ranks.findByName(data.name)
            if (conflict !== null) throw RankConflictException()

            val rank = Rank(
                _id = UUID.randomUUID().toString(),
                name = data.name,
                nameLower = data.name.lowercase(),
                displayName = data.displayName,
                priority = data.priority,
                prefix = data.prefix,
                permissions = data.permissions.distinct(),
                staff = data.staff,
                applyOnJoin = data.applyOnJoin,
                createdAt = System.currentTimeMillis()
            )

            Database.ranks.save(rank)

            call.respond(rank)
        }
    }

    get {
        val ranks = Database.ranks.find().toList()
        call.respond(ranks)
    }

    get("/{id}") {
        val id = call.parameters["id"] ?: throw ValidationException()
        val rank = Database.ranks.findByIdOrName(id) ?: throw RankMissingException()
        call.respond(rank)
    }

    delete("/{id}") {
        val id = call.parameters["id"] ?: throw ValidationException()
        val result = Database.ranks.deleteById(id)
        if (result.deletedCount == 0L) throw RankMissingException()
        call.respond(Unit)

        val playersWithRank = Database.players.find(Player::rankIds contains id).toList()
        playersWithRank.forEach {
            it.rankIds = it.rankIds.filterNot { rankId -> rankId == id }
            Database.players.save(it)
        }
    }

    put("/{id}") {
        val id = call.parameters["id"] ?: throw ValidationException()
        validate<RankUpdateRequest>(this) { data ->
            val existingRank = Database.ranks.findById(id) ?: throw RankMissingException()
            val conflictRank =
                Database.ranks.findOne(not(Rank::_id eq existingRank._id), Rank::nameLower eq data.name.lowercase())
            if (conflictRank != null) throw RankConflictException()

            val updatedRank = Rank(
                _id = existingRank._id,
                createdAt = existingRank.createdAt,
                name = data.name,
                nameLower = data.name.lowercase(),
                displayName = data.displayName,
                prefix = data.prefix,
                priority = data.priority,
                permissions = data.permissions.distinct(),
                staff = data.staff,
                applyOnJoin = data.applyOnJoin
            )

            Database.ranks.updateOne(Rank::_id eq existingRank._id, updatedRank)

            call.respond(updatedRank)
        }
    }
}

fun Application.rankRoutes() {
    routing {
        route("/mc/ranks") {
            manageRanks()
        }
    }
}
