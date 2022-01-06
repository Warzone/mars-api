package network.warzone.api.socket.match

import kotlinx.serialization.Serializable
import network.warzone.api.database.models.Match
import network.warzone.api.database.models.PlayerMessages
import network.warzone.api.database.models.SimpleParticipant

@Serializable
data class MatchStartData(val participants: Set<SimpleParticipant>)

@Serializable
data class MatchEndData(val winningParties: List<String>, val bigStats: Map<String, BigStats>) {
    @Serializable
    data class BigStats(
        var blocks: PlayerBlocksData = PlayerBlocksData(),
        var messages: PlayerMessages = PlayerMessages(),
        var bowShotsTaken: Int = 0,
        var bowShotsHit: Int = 0,
        var damageGiven: Double = 0.0,
        var damageTaken: Double = 0.0,
        var damageGivenBow: Double = 0.0
    ) {
        @Serializable
        data class PlayerBlocksData(
            var blocksPlaced: Map<String, Int> = mutableMapOf(),
            var blocksBroken: Map<String, Int> = mutableMapOf()
        )
    }

    fun isTie(match: Match): Boolean {
        return winningParties.isEmpty() || winningParties.count() == match.parties.count()
    }
}