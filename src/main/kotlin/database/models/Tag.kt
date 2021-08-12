package network.warzone.api.database.models

import kotlinx.serialization.Serializable

@Serializable
data class Tag(val _id: String, var name: String, var nameLower: String, var display: String, val createdAt: Long)