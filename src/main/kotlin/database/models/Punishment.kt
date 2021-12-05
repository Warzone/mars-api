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
    val punisher: SimplePlayer,
    val target: SimplePlayer,
    val targetIps: List<String>,
    val reversion: PunishmentReversion? = null
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
data class StaffNote(val author: SimplePlayer, val content: String, val createdAt: Long)

@Serializable
data class PunishmentReason(val name: String, val message: String, val short: String)

@Serializable
data class PunishmentReversion(val revertedAt: Long, val note: StaffNote, val reverter: SimplePlayer, val reason: ReversionReason)

@Serializable
enum class ReversionReason {
    FALSE_PUNISHMENT,
    COMPASSIONATE
}