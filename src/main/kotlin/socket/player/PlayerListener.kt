package network.warzone.api.socket.player

import network.warzone.api.socket.match.MatchEndData
import network.warzone.api.socket.participant.PlayerMatchResult

open class PlayerListener<T> {
    open suspend fun onKill(context: T, data: PlayerDeathData, firstBlood: Boolean): T {
        return context
    }

    open suspend fun onDeath(context: T, data: PlayerDeathData, firstBlood: Boolean): T {
        return context
    }

    open suspend fun onChat(context: T, data: PlayerChatData): T {
        return context
    }

    open suspend fun onKillstreak(context: T, amount: Int): T {
        return context
    }

    open suspend fun onPartyJoin(context: T, partyName: String): T {
        return context
    }

    open suspend fun onPartyLeave(context: T): T {
        return context
    }

    open suspend fun onMatchEnd(context: T, data: MatchEndData, bigStats: MatchEndData.BigStats, result: PlayerMatchResult): T {
        return context
    }

    open suspend fun onDestroyableDestroy(context: T, percentage: Float, blockCount: Int): T {
        return context
    }

    open suspend fun onCoreLeak(context: T, percentage: Float, blockCount: Int): T {
        return context
    }

    open suspend fun onFlagPlace(context: T, heldTime: Long): T {
        return context
    }

    open suspend fun onFlagDrop(context: T, heldTime: Long): T {
        return context
    }

    open suspend fun onFlagPickup(context: T): T {
        return context
    }

    open suspend fun onFlagDefend(context: T): T {
        return context
    }

    open suspend fun onWoolPlace(context: T, heldTime: Long): T {
        return context
    }

    open suspend fun onWoolPickup(context: T): T {
        return context
    }

    open suspend fun onWoolDrop(context: T, heldTime: Long): T {
        return context
    }

    open suspend fun onWoolDefend(context: T): T {
        return context
    }

    open suspend fun onControlPointCapture(context: T, contributors: Int): T {
        return context
    }
}