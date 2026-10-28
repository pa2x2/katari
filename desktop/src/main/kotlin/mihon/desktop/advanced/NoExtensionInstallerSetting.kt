package mihon.desktop.advanced

import androidx.compose.runtime.Composable
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.advanced.ExtensionInstallerSetting

/**
 * Android chooses between the system's APK installers; desktop loads no extensions yet, so there is no installer to
 * choose. Stands in until desktop hosts extensions.
 */
@Inject
class NoExtensionInstallerSetting : ExtensionInstallerSetting {
    @Composable
    override fun preferenceItem(): Preference.PreferenceItem<out Any, out Any>? = null
}
