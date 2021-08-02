package network.warzone.api.http.rank

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import network.warzone.api.database.Database
import network.warzone.api.database.model.Rank
import network.warzone.api.http.RankConflictException
import network.warzone.api.http.RankMissingException
import network.warzone.api.http.ValidationException
import network.warzone.api.util.validate
import org.litote.kmongo.eq
import org.litote.kmongo.or
import java.util.*

fun Route.manageRanks() {
    post {
        validate<RankCreateRequest>(this) { data ->
            val conflict = Database.ranks.findOne(Rank::name eq data.name.toLowerCase())
            if (conflict !== null) throw RankConflictException()

            val rank = Rank(
                _id = UUID.randomUUID().toString(),
                name = data.name.toLowerCase(),
                displayName = data.displayName,
                priority = data.priority,
                prefix = data.prefix,
                permissions = data.permissions,
                staff = data.staff,
                applyOnJoin = data.applyOnJoin,
                createdAt = System.currentTimeMillis()
            )

            println(rank)

            Database.ranks.save(rank)

            call.respond(RankCreateResponse(rank))
        }
    }

    get {
        val ranks = Database.ranks.find().toList()
        call.respond(RankListResponse(ranks))
    }

    get("/{id}") {
        val id = call.parameters["id"] ?: throw ValidationException()
        val rank = Database.ranks.findOne(or(Rank::_id eq id, Rank::name eq id.toLowerCase()))
            ?: throw RankMissingException()
        call.respond(RankCreateResponse(rank))
    }

    delete("/{id}") {
        val id = call.parameters["id"] ?: throw ValidationException()
        val result = Database.ranks.deleteOne(or(Rank::_id eq id, Rank::name eq id.toLowerCase()))
        if (result.deletedCount == 0L) throw RankMissingException()
        call.respond(Unit)
    }

    put("/{id}") {
        val id = call.parameters["id"]?.toLowerCase() ?: throw ValidationException()
        validate<RankUpdateRequest>(this) { data ->
            val existingRank = Database.ranks.findOne(or(Rank::_id eq id, Rank::name eq id))
                ?: throw RankMissingException()

            val updatedRank = Rank(
                _id = existingRank._id,
                createdAt = existingRank.createdAt,
                name = data.name,
                displayName = data.displayName,
                prefix = data.prefix,
                priority = data.priority,
                permissions = data.permissions,
                staff = data.staff,
                applyOnJoin = data.applyOnJoin
            )

            Database.ranks.updateOne(Rank::_id eq existingRank._id, updatedRank)

            call.respond(RankCreateResponse(updatedRank))
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
