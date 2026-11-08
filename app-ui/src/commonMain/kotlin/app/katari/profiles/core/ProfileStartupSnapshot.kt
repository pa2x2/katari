package app.katari.profiles.core

data class ProfileStartupSnapshot(
    val initialProfile: Profile?,
    val visibleProfiles: List<Profile>,
    val shouldShowPicker: Boolean,
)
