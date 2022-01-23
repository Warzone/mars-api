package network.warzone.api.database.models

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class Punishment(
    val _id: String,
    val reason: PunishmentReason,
    val issuedAt: Long,
    val silent: Boolean,
    val offence: Int,
    val action: PunishmentAction,
    val note: String? = null,
    val punisher: SimplePlayer? = null,
    val target: SimplePlayer,
    val targetIps: List<String>,
    var reversion: PunishmentReversion? = null,
    val serverId: String? = null
) {
    val expiresAt: Long
        get() {
            return if (action.length == -1L) -1L else issuedAt + action.length
        }

    val isActive: Boolean
        get() {
            if (reversion != null) return false
            return action.length == -1L || Date().time < expiresAt
        }
}

@Serializable
data class StaffNote(val id: Int, val author: SimplePlayer, val content: String, val createdAt: Long)

@Serializable
data class PunishmentReason(val name: String, val message: String, val short: String)

@Serializable
data class PunishmentReversion(val revertedAt: Long, val reverter: SimplePlayer, val reason: String)