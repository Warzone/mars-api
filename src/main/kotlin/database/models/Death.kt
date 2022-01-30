package network.warzone.api.database.models

import kotlinx.serialization.Serializable

@Serializable
data class Death(
    val _id: String,
    val victim: SimplePlayer,
    val attacker: SimplePlayer? = null,
    val weapon: String? = null,
    val entity: String? = null,
    val distance: Int? = null,
    val key: String,
    val cause: DamageCause,
    val serverId: String,
    val matchId: String,
    val createdAt: Long
)

@Serializable
enum class DamageCause {
    MELEE,
    PROJECTILE,
    EXPLOSION,
    FIRE,
    LAVA,
    POTION,
    FLATTEN,
    FALL,
    PRICK,
    DROWN,
    STARVE,
    SUFFOCATE,
    SHOCK,
    SPLEEF,
    VOID,
    UNKNOWN
}