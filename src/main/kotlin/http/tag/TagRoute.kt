package network.warzone.api.http.tag

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import network.warzone.api.database.Database
import network.warzone.api.database.model.Player
import network.warzone.api.database.model.Tag
import network.warzone.api.http.*
import network.warzone.api.util.validate
import org.litote.kmongo.contains
import org.litote.kmongo.eq
import java.util.*

fun Route.manageTags() {
    post {
        validate<TagCreateRequest>(this) { data ->
            val conflict = Tag.findByName(data.name)
            if (conflict !== null) throw TagConflictException()

            val tag = Tag(
                _id = UUID.randomUUID().toString(),
                name = data.name,
                nameLower = data.name.toLowerCase(),
                display = data.display,
                createdAt = System.currentTimeMillis()
            )

            Database.tags.save(tag)

            call.respond(TagCreateResponse(tag))
        }
    }

    get {
        val tags = Database.tags.find().toList()
        call.respond(TagListResponse(tags))
    }

    get("/{id}") {
        val id = call.parameters["id"] ?: throw ValidationException()
        val tag = Tag.findByIdOrName(id) ?: throw TagMissingException()
        call.respond(TagCreateResponse(tag))
    }

    delete("/{id}") {
        val id = call.parameters["id"] ?: throw ValidationException()
        val result = Tag.deleteById(id)
        if (result.deletedCount == 0L) throw TagMissingException()
        call.respond(Unit)

        val playersWithTag = Database.players.find(Player::tagIds contains id).toList()
        playersWithTag.forEach {
            it.tagIds = it.tagIds.filterNot { tagId -> tagId == id }
            Database.players.save(it)
        }
    }

    put("/{id}") {
        val id = call.parameters["id"] ?: throw ValidationException()
        validate<TagCreateRequest>(this) { data ->
            val existingTag = Tag.findByIdOrName(id) ?: throw TagMissingException()

            val updatedTag = Tag(
                _id = existingTag._id,
                createdAt = existingTag.createdAt,
                name = data.name,
                nameLower = data.name.toLowerCase(),
                display = data.display,
            )

            Database.tags.updateOne(Tag::_id eq existingTag._id, updatedTag)

            call.respond(TagCreateResponse(updatedTag))
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
