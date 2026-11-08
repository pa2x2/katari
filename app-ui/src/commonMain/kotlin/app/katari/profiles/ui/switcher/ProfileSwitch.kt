package app.katari.profiles.ui.switcher

import app.katari.core.platform.PlatformContext
import app.katari.profiles.core.Profile
import app.katari.profiles.core.ProfileManager
import app.katari.profiles.ui.ProfileUnlock

/** Makes [profile] the active one after unlocking it if it's locked; false when the unlock didn't happen. */
suspend fun switchToProfile(
    context: PlatformContext,
    profileManager: ProfileManager,
    profileUnlock: ProfileUnlock,
    switchEffects: ProfileSwitchEffects,
    profile: Profile,
    onBeforeSwitch: () -> Unit = {},
): Boolean {
    if (profileManager.profileRequiresAuthNow(profile.id)) {
        if (!profileUnlock.unlock(context, listOf(profile.name))) return false
        profileManager.markProfileAuthenticated(profile.id)
        switchEffects.unlocked()
    }
    onBeforeSwitch()
    profileManager.setActiveProfile(profile.id)
    switchEffects.switched()
    return true
}
