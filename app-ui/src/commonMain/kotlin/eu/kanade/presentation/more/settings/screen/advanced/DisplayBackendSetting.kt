package eu.kanade.presentation.more.settings.screen.advanced

import androidx.compose.runtime.Composable
import eu.kanade.presentation.more.settings.Preference

/** The choice of how the app's windows reach the display, which the advanced settings offer. */
interface DisplayBackendSetting {
    /** The row with whatever dialogs it opens; null where the platform reaches the display in one way only. */
    @Composable
    fun preferenceItem(): Preference.PreferenceItem<out Any, out Any>?
}
