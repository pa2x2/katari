package mihon.feature.profiles.ui

import android.content.Context
import androidx.fragment.app.FragmentActivity
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.setAppCompatDelegateThemeMode
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil.authenticate
import eu.kanade.tachiyomi.util.system.toast
import mihon.feature.profiles.core.Profile
import mihon.feature.profiles.core.ProfileManager
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR

internal fun resolveProfileShortcutTarget(
    profiles: List<Profile>,
    activeProfileId: Long?,
): Profile? {
    if (activeProfileId == null || profiles.size != 2) return null
    return profiles.singleOrNull { it.id != activeProfileId }
}

internal fun ProfileManager.resolveProfileShortcutTarget(): Profile? {
    val currentProfileId = activeProfile.value?.id ?: activeProfileId
    return resolveProfileShortcutTarget(visibleProfiles.value, currentProfileId)
}

internal suspend fun switchToProfile(
    context: Context,
    profileManager: ProfileManager,
    uiPreferences: UiPreferences,
    profile: Profile,
    showToast: Boolean = false,
    onBeforeSwitch: () -> Unit = {},
): Boolean {
    val authenticated = if (profileManager.profileRequiresUnlock(profile.id) && context is FragmentActivity) {
        context.authenticate(
            title = context.stringResource(MR.strings.unlock_app_title, profile.name),
            subtitle = null,
        )
    } else {
        true
    }
    if (!authenticated) return false

    onBeforeSwitch()
    profileManager.setActiveProfile(profile.id)
    setAppCompatDelegateThemeMode(uiPreferences.themeMode.get())
    if (showToast) {
        context.toast(context.stringResource(MR.strings.profiles_switched, profile.name))
    }
    return true
}

internal suspend fun handleProfileShortcut(
    context: Context,
    profileManager: ProfileManager,
    uiPreferences: UiPreferences,
    onOpenProfilePicker: () -> Unit,
    onBeforeSwitch: () -> Unit = {},
): Boolean {
    val nextProfile = profileManager.resolveProfileShortcutTarget()
    if (nextProfile == null) {
        onOpenProfilePicker()
        return false
    }

    return switchToProfile(
        context = context,
        profileManager = profileManager,
        uiPreferences = uiPreferences,
        profile = nextProfile,
        showToast = true,
        onBeforeSwitch = onBeforeSwitch,
    )
}
