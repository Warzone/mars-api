package network.warzone.api.database.models

import kotlinx.serialization.Serializable

@Serializable
data class LevelColor(val level: Int, val color: String)
