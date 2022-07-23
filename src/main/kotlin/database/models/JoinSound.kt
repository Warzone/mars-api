package network.warzone.api.database.models

import kotlinx.serialization.Serializable

@Serializable
data class JoinSound(
    val id: String,
    val name: String,
    val description: List<String>,
    val sound: String,
    val permission: String,
    val guiIcon: String,
    val guiSlot: Int,
    val volume: Float = 1.0f,
    val pitch: Float = 1.0f
)