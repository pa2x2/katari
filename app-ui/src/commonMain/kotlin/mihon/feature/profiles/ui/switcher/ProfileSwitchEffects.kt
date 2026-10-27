package mihon.feature.profiles.ui.switcher

/** What the platform does around a profile switch the user makes, besides making the profile active. */
interface ProfileSwitchEffects {
    /** The user has just authenticated to enter a locked profile. */
    fun unlocked()

    /** The profile the user switched to is now active. */
    fun switched()
}
