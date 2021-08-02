package network.warzone.api.database.model

import kotlinx.serialization.Serializable
import network.warzone.api.database.Database
import org.litote.kmongo.eq
import org.litote.kmongo.or

@Serializable
data class Player(
    var _id: String,
    var name: String,
    var nameLower: String,
    var firstJoinedAt: Long,
    var lastJoinedAt: Long,
    var playtime: Long,
    var ips: List<String>,
    var rankIds: List<String>
) {
    suspend fun getActiveSession(): Session? {
        return Database.sessions.findOne(Session::endedAt eq null, Session::playerId eq _id)
    }

    companion object {
        suspend fun findByIdOrName(id: String): Player? {
            return Database.players.findOne(or(Player::_id eq id, Player::nameLower eq id.toLowerCase()))
        }

        suspend fun findById(id: String): Player? {
            return Database.players.findOne(Player::_id eq id)
        }
    }
}