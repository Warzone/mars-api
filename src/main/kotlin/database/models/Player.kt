package network.warzone.api.database.models

import kotlinx.serialization.Serializable
import network.warzone.api.database.Database
import org.litote.kmongo.SetTo
import org.litote.kmongo.and
import org.litote.kmongo.eq
import org.litote.kmongo.not

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

    companion object {
        suspend fun ensureNameUniqueness(name: String, keepId: String) {
            val tempName = ">>awarzoneplayer${(0..1000).random()}"
            Database.players.updateMany(and(Player::nameLower eq name.lowercase(), not(Player::_id eq keepId)), SetTo(Player::name, tempName), SetTo(Player::nameLower, tempName))
        }
    }
}

@Serializable
data class SimplePlayer(val name: String, val id: String)