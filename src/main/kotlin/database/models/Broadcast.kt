package network.warzone.api.database.models

import kotlinx.serialization.Serializable

@Serializable
data class Broadcast(val name: String, val message: String, val permission: String? = null, val newline: Boolean = true)