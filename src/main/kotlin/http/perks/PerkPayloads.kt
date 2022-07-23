package network.warzone.api.http.perks

import kotlinx.serialization.Serializable

@Serializable
data class JoinSoundSetRequest(val activeJoinSoundId: String? = null)