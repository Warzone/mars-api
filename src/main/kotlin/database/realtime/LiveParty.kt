package network.warzone.api.database.realtime

import kotlinx.serialization.Serializable
import network.warzone.api.socket.ConnectionStore

@Serializable
data class LiveParty(
    val name: String,
    val alias: String,
    val colour: String,
    val min: Int,
    val max: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (other is LiveParty) return other.name == name
        return false
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}
