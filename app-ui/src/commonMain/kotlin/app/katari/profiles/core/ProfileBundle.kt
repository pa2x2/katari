package app.katari.profiles.core

data class ProfileBundle(
    val profile: Profile,
    val categories: List<Long>,
    val mangaCount: Int,
)
