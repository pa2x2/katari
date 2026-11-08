package mihon.desktop.advanced

import androidx.compose.runtime.Composable
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.advanced.HardwareBitmapSetting

/** Desktop decodes pages into ordinary bitmaps only, so there is no hardware bitmap size to limit. */
@Inject
class NoHardwareBitmapSetting : HardwareBitmapSetting {
    @Composable
    override fun preferenceItem(): Preference.PreferenceItem<out Any, out Any>? = null
}
