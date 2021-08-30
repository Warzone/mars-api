package network.warzone.api.socket2.listeners.party

import kotlinx.serialization.Serializable

@Serializable
data class LiveParty(
    val name: String,
    val alias: String,
    val colour: String,
    val min: Int,
    val max: Int,
)
