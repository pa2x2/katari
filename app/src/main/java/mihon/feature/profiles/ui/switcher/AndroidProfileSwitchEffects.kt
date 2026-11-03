package mihon.feature.profiles.ui.switcher

import dev.zacsweers.metro.Inject
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.setAppCompatDelegateThemeMode
import eu.kanade.tachiyomi.ui.base.delegate.SecureActivityDelegate

/**
 * The profile's unlock also satisfies the app lock, and AppCompat's night mode, which composition does not drive, is
 * set to the theme of the profile switched to.
 */
@Inject
class AndroidProfileSwitchEffects(private val uiPreferences: UiPreferences) : ProfileSwitchEffects {
    override fun unlocked() = SecureActivityDelegate.unlock()

    override fun switched() = setAppCompatDelegateThemeMode(uiPreferences.themeMode.get())
}
