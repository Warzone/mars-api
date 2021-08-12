package network.warzone.api.database.models

import kotlinx.serialization.Serializable

@Serializable
data class Map(
    val _id: String,
    var name: String,
    var nameLower: String,
    var version: String,
    var gamemodes: List<MapGamemode>,
    val loadedAt: Long,
    var updatedAt: Long,
    var authors: List<MapContributor>,
    var contributors: List<MapContributor>
)

@Serializable
data class MapContributor(val uuid: String, var contribution: String? = null)

@Serializable
enum class MapGamemode(val fancy: String) {
    ATTACK_DEFEND("Attack/Defend"),
    ARCADE("Arcade"),
    BLITZ("Blitz"),
    BLITZ_RAGE("Blitz: Rage"),
    CAPTURE_THE_FLAG("Capture the Flag"),
    CONTROL_THE_POINT("Control the Point"),
    CAPTURE_THE_WOOL("Capture the Wool"),
    DESTROY_THE_CORE("Destroy the Core"),
    DESTROY_THE_MONUMENT("Destroy the Monument"),
    FREE_FOR_ALL("Free For All"),
    FLAG_FOOTBALL("Flag Football"),
    KING_OF_THE_HILL("King of the Hill"),
    KING_OF_THE_FLAG("King of the Flag"),
    MIXED("Mixed"),
    RAGE("Rage"),
    RACE_FOR_WOOL("Race for Wool"),
    SCOREBOX("Scorebox"),
    DEATHMATCH("Deathmatch");
}