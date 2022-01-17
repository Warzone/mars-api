package network.warzone.api.socket

import kotlinx.serialization.Serializable

@Serializable
enum class EventType {
    MATCH_LOAD,
    MATCH_START,
    MATCH_END,
    FORCE_MATCH_END, // plugin-bound

    PLAYER_DEATH,
    PLAYER_CHAT, // bi-directional
    PLAYER_XP_GAIN, // plugin-bound
    MESSAGE, // plugin-bound
    PROJECTILE_HIT,
    KILLSTREAK,

    PARTY_JOIN,
    PARTY_LEAVE,

    DESTROYABLE_DESTROY,
    DESTROYABLE_DAMAGE,

    CORE_LEAK,
    CORE_DAMAGE,

    FLAG_CAPTURE,
    FLAG_PICKUP,
    FLAG_DROP,
    FLAG_DEFEND,

    WOOL_CAPTURE,
    WOOL_PICKUP,
    WOOL_DROP,
    WOOL_DEFEND,

    CONTROL_POINT_CAPTURE,
}