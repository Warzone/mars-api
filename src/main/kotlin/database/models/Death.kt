package network.warzone.api.database.models

import kotlinx.serialization.Serializable

@Serializable
data class Death(
    val victim: SimplePlayer,
    val attacker: SimplePlayer?,
    val weapon: String?,
    val entity: String?,
    val distance: Int?,
    val key: String,
    val cause: DamageCause
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
    VOID,
    UNKNOWN
}