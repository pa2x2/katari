package eu.kanade.presentation.more.settings.screen.advanced

import androidx.compose.runtime.Composable
import app.katari.core.common.platform.PlatformFacility
import eu.kanade.presentation.more.settings.Preference

/**
 * The largest page the manga reader decodes into a hardware bitmap, which the advanced settings offer among the reader
 * settings.
 */
@PlatformFacility
interface HardwareBitmapSetting {
    /** The row; null where the platform decodes no page into a hardware bitmap. */
    @Composable
    fun preferenceItem(): Preference.PreferenceItem<out Any, out Any>?
}
