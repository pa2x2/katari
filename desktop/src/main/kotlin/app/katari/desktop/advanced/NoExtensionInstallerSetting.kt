package app.katari.desktop.advanced

import androidx.compose.runtime.Composable
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.advanced.ExtensionInstallerSetting

/**
 * Android chooses between the system's APK installers; desktop installs every APK extension as the app's private copy,
 * so there is no installer to choose.
 */
@Inject
class NoExtensionInstallerSetting : ExtensionInstallerSetting {
    @Composable
    override fun preferenceItem(): Preference.PreferenceItem<out Any, out Any>? = null
}
