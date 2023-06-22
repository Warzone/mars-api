package network.warzone.api.socket

import kotlinx.serialization.Serializable

@Serializable
enum class EventType {
    // API-bound
    ACHIEVEMENT_EARN,
    MATCH_LOAD,
    MATCH_START,
    MATCH_END,
    PLAYER_DEATH,
    KILLSTREAK,
    PARTY_JOIN,
    PARTY_LEAVE,
    DESTROYABLE_DESTROY,
    DESTROYABLE_DAMAGE,
    CORE_LEAK,
    CORE_DAMAGE, // unused
    FLAG_CAPTURE,
    FLAG_PICKUP,
    FLAG_DROP,
    FLAG_DEFEND,
    WOOL_CAPTURE,
    WOOL_PICKUP,
    WOOL_DROP,
    WOOL_DEFEND,
    CONTROL_POINT_CAPTURE,

    // bi-directional
    PLAYER_CHAT,

    // plugin-bound
    PLAYER_XP_GAIN,
    FORCE_MATCH_END,
    MESSAGE,
    DISCONNECT_PLAYER
}