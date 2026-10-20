package mihon.feature.profiles.ui.switcher

import android.content.Context
import androidx.fragment.app.FragmentActivity
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.setAppCompatDelegateThemeMode
import eu.kanade.tachiyomi.ui.base.delegate.SecureActivityDelegate
import eu.kanade.tachiyomi.ui.security.BiometricAuthentication.authenticate
import mihon.feature.profiles.core.Profile
import mihon.feature.profiles.core.ProfileManager
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.*

/** Makes [profile] the active one after unlocking it if it's locked; false when the unlock didn't happen. */
internal suspend fun switchToProfile(
    context: Context,
    profileManager: ProfileManager,
    uiPreferences: UiPreferences,
    profile: Profile,
    onBeforeSwitch: () -> Unit = {},
): Boolean {
    if (profileManager.profileRequiresAuthNow(profile.id)) {
        // Without an activity to host the prompt the profile can't be unlocked, so it stays closed.
        val authenticated = context is FragmentActivity &&
            context.authenticate(
                title = context.stringResource(MR.strings.unlock_app_title, profile.name),
                subtitle = null,
            )
        if (!authenticated) return false
        profileManager.markProfileAuthenticated(profile.id)
        SecureActivityDelegate.unlock()
    }
    onBeforeSwitch()
    profileManager.setActiveProfile(profile.id)
    setAppCompatDelegateThemeMode(uiPreferences.themeMode.get())
    return true
}
