package network.warzone.api.database.model

import kotlinx.serialization.Serializable
import network.warzone.api.database.Database
import org.litote.kmongo.eq
import org.litote.kmongo.or

@Serializable
data class Player(
    val _id: String,
    var name: String,
    var nameLower: String,
    var firstJoinedAt: Long,
    var lastJoinedAt: Long,
    var playtime: Long,
    var ips: List<String>,
    var rankIds: List<String>,
    var tagIds: List<String>,
    var activeTagId: String?
) {
    suspend fun getActiveSession(): Session? {
        return Database.sessions.findOne(Session::endedAt eq null, Session::playerId eq _id)
    }
}