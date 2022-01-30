package network.warzone.api.database.models

import kotlinx.serialization.Serializable
import network.warzone.api.database.Database
import org.litote.kmongo.eq

@Serializable
data class Rank(
    val _id: String,
    var name: String,
    var nameLower: String,
    var displayName: String? = null,
    var prefix: String? = null,
    var priority: Int,
    var permissions: List<String>,
    var staff: Boolean,
    var applyOnJoin: Boolean,
    val createdAt: Double
) {
    companion object {
        suspend fun findDefault(): List<Rank> {
            return Database.ranks.find(Rank::applyOnJoin eq true).toList()
        }
    }
}
