package network.warzone.api.socket.player

import network.warzone.api.database.models.GamemodeStats
import network.warzone.api.database.models.Match
import network.warzone.api.database.models.Player
import network.warzone.api.socket.EventType
import network.warzone.api.socket.leaderboard.XPLeaderboard
import network.warzone.api.socket.participant.ParticipantContext

data class PlayerContext(val profile: Player, val match: Match) {
    fun getParticipant(): ParticipantContext {
        val profile = match.participants[profile._id]!!
        return ParticipantContext(profile, match)
    }

    /**
     * Calls the provided lambda for every gamemode of the match level, with the player's current gamemode stats as the parameter
     *
     * @return Modified PlayerContext (no DB change)
     *
     */
    suspend fun modifyGamemodeStats(modify: suspend (gamemodeStats: GamemodeStats) -> GamemodeStats): PlayerContext {
        match.level.gamemodes.forEach {
            profile.gamemodeStats[it] = modify(profile.gamemodeStats[it] ?: GamemodeStats())
        }

        return this
    }

    /**
     * Sends a message to the player in-game with an optional Bukkit Sound
     */
    suspend fun sendMessage(message: String, sound: String? = null) {
        match.server.call(EventType.MESSAGE, MessageData(message, sound, listOf(profile._id)))
    }

    suspend fun addXP(rawXP: Int, reason: String, notify: Boolean = true, rawOnly: Boolean = false): PlayerContext {
        val originalLevel = profile.stats.level

        val xp = if (rawOnly) rawXP else gain(rawXP, originalLevel)
        profile.stats.xp += xp

        // Notify the MC server of the XP gain
        match.server.call(EventType.PLAYER_XP_GAIN, PlayerXPGainData(profile._id, xp, reason, notify))

        // Update XP leaderboard score
        XPLeaderboard.increment(profile.idName, xp)

        return this
    }
}