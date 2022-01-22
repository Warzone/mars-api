package network.warzone.api.database.models

import kotlinx.serialization.Serializable

// Also known as a "map" in-game
@Serializable
data class Level(
    val _id: String,
    val loadedAt: Long,
    var name: String,
    var nameLower: String,
    var version: String,
    var gamemodes: List<LevelGamemode>,
    var updatedAt: Long,
    var authors: List<LevelContributor>,
    var contributors: List<LevelContributor>,
    var goals: GoalCollection? = null,
    var lastMatchId: String? = null,
    val records: LevelRecords
)


@Serializable
data class LevelRecords(
    var highestKillstreak: IntRecord? = null,
    var longestProjectileKill: ProjectileRecord? = null,
    var fastestWoolCapture: LongRecord? = null,
    var fastestFlagCapture: LongRecord? = null,
    var fastestFirstBlood: FirstBloodRecord? = null,
    var killsInMatch: IntRecord? = null,
    var deathsInMatch: IntRecord? = null,
)

@Serializable
data class LevelContributor(val uuid: String, var contribution: String? = null)

@Serializable
enum class LevelGamemode(val fancy: String) {
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