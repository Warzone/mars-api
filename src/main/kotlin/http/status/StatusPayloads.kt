package network.warzone.api.http.status

import kotlinx.serialization.Serializable

@Serializable
data class StatusResponse(
    val status: String
)