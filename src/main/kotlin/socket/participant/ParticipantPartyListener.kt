package network.warzone.api.socket.participant

import network.warzone.api.socket.player.PlayerListener
import java.util.*

class ParticipantPartyListener : PlayerListener<ParticipantContext>() {
    override suspend fun onPartyJoin(context: ParticipantContext, partyName: String): ParticipantContext {
        val (profile) = context
        profile.partyName = partyName
        profile.lastPartyName = partyName
        profile.joinedPartyAt = Date().time
        return context
    }

    override suspend fun onPartyLeave(context: ParticipantContext): ParticipantContext {
        val (profile) = context
        profile.partyName = null
        profile.lastLeftPartyAt = Date().time
        profile.joinedPartyAt = null
        return context
    }
}