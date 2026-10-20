package mihon.feature.profiles.ui.switcher

import android.content.Context
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.setAppCompatDelegateThemeMode
import eu.kanade.tachiyomi.ui.base.delegate.SecureActivityDelegate
import mihon.feature.profiles.core.Profile
import mihon.feature.profiles.core.ProfileManager
import mihon.feature.profiles.ui.unlockProfiles

/** Makes [profile] the active one after unlocking it if it's locked; false when the unlock didn't happen. */
internal suspend fun switchToProfile(
    context: Context,
    profileManager: ProfileManager,
    uiPreferences: UiPreferences,
    profile: Profile,
    onBeforeSwitch: () -> Unit = {},
): Boolean {
    if (profileManager.profileRequiresAuthNow(profile.id)) {
        if (!unlockProfiles(context, listOf(profile.name))) return false
        profileManager.markProfileAuthenticated(profile.id)
        SecureActivityDelegate.unlock()
    }
    onBeforeSwitch()
    profileManager.setActiveProfile(profile.id)
    setAppCompatDelegateThemeMode(uiPreferences.themeMode.get())
    return true
}
