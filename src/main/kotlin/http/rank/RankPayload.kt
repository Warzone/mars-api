package network.warzone.api.http.rank

import kotlinx.serialization.Serializable
import network.warzone.api.database.model.Rank

@Serializable
data class RankCreateRequest(
    val name: String,
    val displayName: String? = null,
    val priority: Int = 0,
    val prefix: String? = null,
    val permissions: List<String> = listOf(),
    val staff: Boolean = false,
    val applyOnJoin: Boolean = false
)

@Serializable
data class RankCreateResponse(val rank: Rank)

@Serializable
data class RankListResponse(val ranks: List<Rank>)

@Serializable
data class RankUpdateRequest(
    val name: String,
    val displayName: String?,
    val priority: Int,
    val prefix: String?,
    val permissions: List<String>,
    val staff: Boolean,
    val applyOnJoin: Boolean
)