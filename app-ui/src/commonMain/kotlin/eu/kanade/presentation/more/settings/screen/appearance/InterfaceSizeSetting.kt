package eu.kanade.presentation.more.settings.screen.appearance

import androidx.compose.runtime.Composable
import eu.kanade.presentation.more.settings.Preference

/** The size the interface is drawn at against the system's own, which the display settings offer. */
interface InterfaceSizeSetting {
    /** The row; null where the system's display size is the only one. */
    @Composable
    fun preferenceItem(): Preference.PreferenceItem<out Any, out Any>?
}
