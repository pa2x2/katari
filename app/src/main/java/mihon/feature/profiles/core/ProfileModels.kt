package mihon.feature.profiles.core

data class ProfileBundle(
    val profile: Profile,
    val categories: List<Long>,
    val mangaCount: Int,
)

data class PendingProfileIntent(
    val action: String?,
)
