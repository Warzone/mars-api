package network.warzone.api.socket.achievement

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import network.warzone.api.database.models.DestroyableGoal
import network.warzone.api.database.models.Player
import network.warzone.api.socket.EventType
import network.warzone.api.socket.match.MatchEndData
import network.warzone.api.socket.participant.PlayerMatchResult
import network.warzone.api.socket.player.PlayerChatData
import network.warzone.api.socket.player.PlayerContext
import network.warzone.api.socket.player.PlayerDeathData
import network.warzone.api.socket.player.PlayerListener
import network.warzone.api.socket.server.ServerContext

@Serializable
enum class PlayerUpdateReason {
    KILL,
    DEATH,
    CHAT,
    KILLSTREAK,
    KILLSTREAK_END,
    PARTY_JOIN,
    PARTY_LEAVE,
    MATCH_END,
    DESTROYABLE_DAMAGE,
    DESTROYABLE_DESTROY,
    CORE_LEAK,
    FLAG_PLACE,
    FLAG_DROP,
    FLAG_PICKUP,
    FLAG_DEFEND,
    WOOL_PLACE,
    WOOL_DROP,
    WOOL_PICKUP,
    WOOL_DEFEND,
    CONTROL_POINT_CAPTURE,
}

@Serializable
sealed class PlayerUpdateData {
    @SerialName("KillUpdateData")
    @Serializable
    data class KillUpdateData(val data: PlayerDeathData, val firstBlood: Boolean) : PlayerUpdateData()
    @SerialName("ChatUpdateData")
    @Serializable
    data class ChatUpdateData(val data: PlayerChatData) : PlayerUpdateData()
    @SerialName("KillstreakUpdateData")
    data class KillstreakUpdateData(val amount: Int) : PlayerUpdateData()
    @Serializable
    @SerialName("PartyUpdateData")
    data class PartyUpdateData(val party: String) : PlayerUpdateData()
    @SerialName("MatchEndUpdateData")
    @Serializable
    data class MatchEndUpdateData(val data: MatchEndData) : PlayerUpdateData()
    @SerialName("DestroyableDamageUpdateData")
    @Serializable
    data class DestroyableDamageUpdateData(val blockCount: Int) : PlayerUpdateData()
    @SerialName("DestroyableDestroyUpdateData")
    @Serializable
    data class DestroyableDestroyUpdateData(val percentage: Float, val blockCount: Int) : PlayerUpdateData()
    @SerialName("CoreLeakUpdateData")
    @Serializable
    data class CoreLeakUpdateData(val percentage: Float, val blockCount: Int) : PlayerUpdateData()
    @SerialName("MonumentPlaceUpdateData")
    @Serializable
    data class MonumentPlaceUpdateData(val heldTime: Long) : PlayerUpdateData()
    @SerialName("MonumentDropUpdateData")
    @Serializable
    data class MonumentDropUpdateData(val heldTime: Long) : PlayerUpdateData()
    @SerialName("ControlPointCaptureUpdateData")
    @Serializable
    data class ControlPointCaptureUpdateData(val contributors: Int) : PlayerUpdateData()
    @SerialName("NoArgs")
    @Serializable
    object NoArgs : PlayerUpdateData()
}

@Serializable
data class PlayerUpdate(
    val updated: Player,
    val data: PlayerUpdateData,
    val reason: PlayerUpdateReason
)

class PlayerUpdateListener(private val server: ServerContext) : PlayerListener<PlayerContext>() {
    override suspend fun onKill(context: PlayerContext, data: PlayerDeathData, firstBlood: Boolean): PlayerContext {
        reply(context.profile, PlayerUpdateData.KillUpdateData(data, firstBlood), PlayerUpdateReason.KILL)
        return super.onKill(context, data, firstBlood)
    }

    override suspend fun onDeath(context: PlayerContext, data: PlayerDeathData, firstBlood: Boolean): PlayerContext {
        reply(context.profile, PlayerUpdateData.KillUpdateData(data, firstBlood), PlayerUpdateReason.DEATH)
        return super.onDeath(context, data, firstBlood)
    }

    override suspend fun onChat(context: PlayerContext, data: PlayerChatData): PlayerContext {
        reply(context.profile, PlayerUpdateData.ChatUpdateData(data), PlayerUpdateReason.CHAT)
        return super.onChat(context, data)
    }

    override suspend fun onKillstreak(context: PlayerContext, amount: Int): PlayerContext {
        reply(context.profile, PlayerUpdateData.KillstreakUpdateData(amount), PlayerUpdateReason.KILLSTREAK)
        return super.onKillstreak(context, amount)
    }

    override suspend fun onKillstreakEnd(context: PlayerContext, amount: Int): PlayerContext {
        reply(context.profile, PlayerUpdateData.KillstreakUpdateData(amount), PlayerUpdateReason.KILLSTREAK_END)
        return super.onKillstreakEnd(context, amount)
    }

    override suspend fun onPartyJoin(context: PlayerContext, partyName: String): PlayerContext {
        reply(context.profile, PlayerUpdateData.PartyUpdateData(partyName), PlayerUpdateReason.PARTY_JOIN)
        return super.onPartyJoin(context, partyName)
    }

    override suspend fun onPartyLeave(context: PlayerContext): PlayerContext {
        reply(context.profile, PlayerUpdateData.NoArgs, PlayerUpdateReason.PARTY_LEAVE)
        return super.onPartyLeave(context)
    }

    override suspend fun onMatchEnd(
        context: PlayerContext,
        data: MatchEndData,
        bigStats: MatchEndData.BigStats,
        result: PlayerMatchResult
    ): PlayerContext {
        reply(context.profile, PlayerUpdateData.MatchEndUpdateData(data), PlayerUpdateReason.MATCH_END)
        return super.onMatchEnd(context, data, bigStats, result)
    }

    override suspend fun onDestroyableDamage(
        context: PlayerContext,
        destroyable: DestroyableGoal,
        blockCount: Int
    ): PlayerContext {
        reply(context.profile, PlayerUpdateData.DestroyableDamageUpdateData(blockCount), PlayerUpdateReason.DESTROYABLE_DAMAGE)
        return super.onDestroyableDamage(context, destroyable, blockCount)
    }

    override suspend fun onDestroyableDestroy(
        context: PlayerContext,
        percentage: Float,
        blockCount: Int
    ): PlayerContext {
        reply(context.profile, PlayerUpdateData.DestroyableDestroyUpdateData(percentage, blockCount), PlayerUpdateReason.DESTROYABLE_DESTROY)
        return super.onDestroyableDestroy(context, percentage, blockCount)
    }

    override suspend fun onCoreLeak(context: PlayerContext, percentage: Float, blockCount: Int): PlayerContext {
        reply(context.profile, PlayerUpdateData.CoreLeakUpdateData(percentage, blockCount), PlayerUpdateReason.CORE_LEAK)
        return super.onCoreLeak(context, percentage, blockCount)
    }

    override suspend fun onFlagPlace(context: PlayerContext, heldTime: Long): PlayerContext {
        reply(context.profile, PlayerUpdateData.MonumentPlaceUpdateData(heldTime), PlayerUpdateReason.FLAG_PLACE)
        return super.onFlagPlace(context, heldTime)
    }

    override suspend fun onFlagDrop(context: PlayerContext, heldTime: Long): PlayerContext {
        reply(context.profile, PlayerUpdateData.MonumentDropUpdateData(heldTime), PlayerUpdateReason.FLAG_DROP)
        return super.onFlagDrop(context, heldTime)
    }

    override suspend fun onFlagPickup(context: PlayerContext): PlayerContext {
        reply(context.profile, PlayerUpdateData.NoArgs, PlayerUpdateReason.FLAG_PICKUP)
        return super.onFlagPickup(context)
    }

    override suspend fun onFlagDefend(context: PlayerContext): PlayerContext {
        reply(context.profile, PlayerUpdateData.NoArgs, PlayerUpdateReason.FLAG_DEFEND)
        return super.onFlagDefend(context)
    }

    override suspend fun onWoolPlace(context: PlayerContext, heldTime: Long): PlayerContext {
        reply(context.profile, PlayerUpdateData.MonumentPlaceUpdateData(heldTime), PlayerUpdateReason.WOOL_PLACE)
        return super.onWoolPlace(context, heldTime)
    }

    override suspend fun onWoolPickup(context: PlayerContext): PlayerContext {
        reply(context.profile, PlayerUpdateData.NoArgs, PlayerUpdateReason.WOOL_PICKUP)
        return super.onWoolPickup(context)
    }

    override suspend fun onWoolDrop(context: PlayerContext, heldTime: Long): PlayerContext {
        reply(context.profile, PlayerUpdateData.MonumentDropUpdateData(heldTime), PlayerUpdateReason.WOOL_DROP)
        return super.onWoolDrop(context, heldTime)
    }

    override suspend fun onWoolDefend(context: PlayerContext): PlayerContext {
        reply(context.profile, PlayerUpdateData.NoArgs, PlayerUpdateReason.WOOL_DEFEND)
        return super.onWoolDefend(context)
    }

    override suspend fun onControlPointCapture(context: PlayerContext, contributors: Int): PlayerContext {
        reply(context.profile, PlayerUpdateData.ControlPointCaptureUpdateData(contributors), PlayerUpdateReason.CONTROL_POINT_CAPTURE)
        return super.onControlPointCapture(context, contributors)
    }

    private suspend fun reply(player: Player, data: PlayerUpdateData, reason: PlayerUpdateReason) {
        this.server.call(EventType.PLAYER_UPDATE, PlayerUpdate(player, data, reason))
    }
}