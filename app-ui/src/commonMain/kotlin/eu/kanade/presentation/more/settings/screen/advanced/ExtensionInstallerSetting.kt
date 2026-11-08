package eu.kanade.presentation.more.settings.screen.advanced

import androidx.compose.runtime.Composable
import app.katari.core.common.platform.PlatformFacility
import eu.kanade.presentation.more.settings.Preference

/** The choice of how extensions are installed, which the advanced settings offer among the extension settings. */
@PlatformFacility
interface ExtensionInstallerSetting {
    /** The row with whatever dialogs it opens; null where the platform installs extensions in one way only. */
    @Composable
    fun preferenceItem(): Preference.PreferenceItem<out Any, out Any>?
}
