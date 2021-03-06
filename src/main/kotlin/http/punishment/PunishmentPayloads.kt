package network.warzone.api.http.punishment

import kotlinx.serialization.Serializable
import network.warzone.api.database.models.PunishmentAction
import network.warzone.api.database.models.PunishmentReason
import network.warzone.api.database.models.SimplePlayer

@Serializable
data class PunishmentIssueRequest(
    val reason: PunishmentReason,
    val offence: Int,
    val action: PunishmentAction,
    val note: String? = null,
    val punisher: SimplePlayer? = null,
    val targetName: String,
    val targetIps: List<String>,
    val silent: Boolean
)

@Serializable
data class PunishmentRevertRequest(
    val reason: String,
    val reverter: SimplePlayer
)