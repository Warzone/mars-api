package network.warzone.api.http.map

import kotlinx.serialization.Serializable
import network.warzone.api.database.models.Level
import network.warzone.api.database.models.LevelContributor
import network.warzone.api.database.models.LevelGamemode

@Serializable
data class MapLoadOneRequest(
    val _id: String,
    val name: String,
    val version: String,
    val gamemodes: List<LevelGamemode>,
    val authors: List<LevelContributor>,
    val contributors: List<LevelContributor>
) {
    fun isSimilar(level: Level?): Boolean {
        if (level == null) return false
        return this.name == level.name &&
                this.version == level.version &&
                this.gamemodes.containsAll(level.gamemodes) &&
                this.authors.containsAll(level.authors) &&
                this.contributors.containsAll(level.contributors)
    }
}
