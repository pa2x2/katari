package eu.kanade.presentation.more.settings.screen.advanced

import androidx.compose.runtime.Composable
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.Preference

/** Android's windows reach the display through its own window manager only, so there is nothing to choose. */
@Inject
class AndroidDisplayBackendSetting : DisplayBackendSetting {
    @Composable
    override fun preferenceItem(): Preference.PreferenceItem<out Any, out Any>? = null
}
