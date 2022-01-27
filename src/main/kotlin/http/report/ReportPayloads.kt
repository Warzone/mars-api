package network.warzone.api.http.report

import kotlinx.serialization.Serializable
import network.warzone.api.database.models.SimplePlayer

@Serializable
data class ReportCreateRequest(
    val target: SimplePlayer,
    val reporter: SimplePlayer,
    val reason: String,
    val onlineStaff: Set<SimplePlayer>
)