package network.warzone.api.http.player

import kotlinx.serialization.Serializable
import network.warzone.api.util.UUIDSerializer
import network.warzone.api.util.isIPv4
import network.warzone.api.util.isPlayerName
import org.valiktor.validate
import java.util.*

@Serializable
data class PlayerLoginData(
    @Serializable(with = UUIDSerializer::class)
    val playerUuid: UUID,
    val playerName: String,
    val ip: String
) {
    init {
        validate(this) {
            validate(PlayerLoginData::playerName).isPlayerName()
            validate(PlayerLoginData::ip).isIPv4()
        }
    }
}