package network.warzone.api.database.model

import com.mongodb.client.result.DeleteResult
import kotlinx.serialization.Serializable
import network.warzone.api.database.Database
import org.litote.kmongo.eq
import org.litote.kmongo.or

@Serializable
data class Tag(val _id: String, var name: String, var nameLower: String, var display: String, val createdAt: Long) {
    companion object {
        suspend fun findByIdOrName(id: String): Tag? {
            return Database.tags.findOne(or(Tag::_id eq id, Tag::nameLower eq id.toLowerCase()))
        }

        suspend fun findByName(id: String): Tag? {
            return Database.tags.findOne(Tag::nameLower eq id.toLowerCase())
        }

        suspend fun  deleteById(id: String): DeleteResult {
            return Database.tags.deleteOne(Tag::_id eq id)
        }
    }
}