package network.warzone.api.database.model

import kotlinx.serialization.Serializable
import network.warzone.api.database.Database
import org.litote.kmongo.eq

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
    }
}
