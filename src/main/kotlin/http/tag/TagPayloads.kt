package network.warzone.api.http.tag

import kotlinx.serialization.Serializable

@Serializable
data class TagCreateRequest(
    val name: String,
    val display: String,
)
