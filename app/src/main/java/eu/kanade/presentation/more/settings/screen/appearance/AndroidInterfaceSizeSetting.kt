package eu.kanade.presentation.more.settings.screen.appearance

import androidx.compose.runtime.Composable
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.Preference

/** Android draws the app at the display and font size set in the system's settings, so it offers none of its own. */
@Inject
class AndroidInterfaceSizeSetting : InterfaceSizeSetting {
    @Composable
    override fun preferenceItem(): Preference.PreferenceItem<out Any, out Any>? = null
}
