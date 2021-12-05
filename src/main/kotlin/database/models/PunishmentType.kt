package network.warzone.api.database.models

import kotlinx.serialization.Serializable

@Serializable
data class PunishmentType(
    val name: String,
    val short: String,
    val message: String,
    val actions: List<PunishmentAction>,
    val material: String,
    val position: Int,
    val tip: String? = null,
    val requiredPermission: String? = "mars.punish"
)

@Serializable
data class PunishmentAction(val kind: PunishmentKind, val length: Long = 0) {
    val isBan: Boolean
    get() {
        return kind == PunishmentKind.BAN || kind == PunishmentKind.IP_BAN
    }
}

@Serializable
enum class PunishmentKind {
    WARN,
    KICK,
    MUTE,
    BAN,
    IP_BAN
}