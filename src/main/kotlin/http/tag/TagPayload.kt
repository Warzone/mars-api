package network.warzone.api.http.tag

import kotlinx.serialization.Serializable
import network.warzone.api.database.model.Tag

@Serializable
data class TagCreateRequest(
    val name: String,
    val display: String,
)

@Serializable
data class TagCreateResponse(val tag: Tag)

@Serializable
data class TagListResponse(val tags: List<Tag>)