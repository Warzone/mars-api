package network.warzone.api.database.model

import com.mongodb.client.result.DeleteResult
import kotlinx.serialization.Serializable
import network.warzone.api.database.Database
import org.litote.kmongo.eq
import org.litote.kmongo.or

@Serializable
data class Rank(
    val _id: String,
    var name: String,
    var nameLower: String,
    var displayName: String?,
    var prefix: String?,
    var priority: Int,
    var permissions: List<String>,
    var staff: Boolean,
    var applyOnJoin: Boolean,
    val createdAt: Long
) {
    companion object {
        suspend fun findDefault(): List<Rank> {
            return Database.ranks.find(Rank::applyOnJoin eq true).toList()
        }

        suspend fun findByIdOrName(id: String): Rank? {
            return Database.ranks.findOne(or(Rank::_id eq id, Rank::nameLower eq id.toLowerCase()))
        }

        suspend fun findByName(id: String): Rank? {
            return Database.ranks.findOne(Rank::nameLower eq id.toLowerCase())
        }

        suspend fun deleteById(id: String): DeleteResult {
            return Database.ranks.deleteOne(Rank::_id eq id)
        }
    }
}
