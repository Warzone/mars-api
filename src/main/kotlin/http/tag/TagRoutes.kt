package network.warzone.api.http.tag

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import network.warzone.api.database.*
import network.warzone.api.database.models.Player
import network.warzone.api.database.models.Tag
import network.warzone.api.http.TagConflictException
import network.warzone.api.http.TagMissingException
import network.warzone.api.http.ValidationException
import network.warzone.api.util.protected
import network.warzone.api.util.validate
import org.litote.kmongo.contains
import org.litote.kmongo.eq
import java.util.*

fun Route.manageTags() {
    post {
        protected(this) { _ ->
            validate<TagCreateRequest>(this) { data ->
                val conflict = Database.tags.findByName(data.name)
                if (conflict != null) throw TagConflictException()

                val tag = Tag(
                    _id = UUID.randomUUID().toString(),
                    name = data.name,
                    nameLower = data.name.lowercase(),
                    display = data.display,
                    createdAt = Date().time.toDouble()
                )

                Database.tags.save(tag)

                call.respond(tag)
            }
        }
    }

    get {
        val tags = Database.tags.find().toList()
        call.respond(tags)
    }

    get("/{tagId}") {
        val id = call.parameters["tagId"] ?: throw ValidationException()
        val tag = Database.tags.findByIdOrName(id) ?: throw TagMissingException()
        call.respond(tag)
    }

    delete("/{tagId}") {
        protected(this) { _ ->
            val id = call.parameters["tagId"] ?: throw ValidationException()
            val result = Database.tags.deleteById(id)
            if (result.deletedCount == 0L) throw TagMissingException()
            call.respond(Unit)

            val playersWithTag = Database.players.find(Player::tagIds contains id).toList()
            playersWithTag.forEach {
                it.tagIds = it.tagIds.filterNot { tagId -> tagId == id }
                if (it.activeTagId == id) it.activeTagId = null
                PlayerCache.set(it.name, it, persist = true)
            }
            application.log.info(
                "Tag '$id' was deleted. Affected players: ${
                    playersWithTag.joinToString(", ") { "${it._id} (${it.name})" }
                }"
            )
        }
    }

    put("/{tagId}") {
        protected(this) { _ ->
            val id = call.parameters["tagId"] ?: throw ValidationException()
            validate<TagCreateRequest>(this) { data ->
                val existingTag = Database.tags.findByIdOrName(id) ?: throw TagMissingException()

                val updatedTag = Tag(
                    _id = existingTag._id,
                    createdAt = existingTag.createdAt,
                    name = data.name,
                    nameLower = data.name.lowercase(),
                    display = data.display,
                )

                Database.tags.updateOne(Tag::_id eq existingTag._id, updatedTag)

                call.respond(updatedTag)
            }
        }
    }
}

fun Application.tagRoutes() {
    routing {
        route("/mc/tags") {
            manageTags()
        }
    }
}
