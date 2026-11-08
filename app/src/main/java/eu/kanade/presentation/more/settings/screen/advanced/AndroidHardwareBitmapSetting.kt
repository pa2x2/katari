package eu.kanade.presentation.more.settings.screen.advanced

import androidx.compose.runtime.Composable
import dev.zacsweers.metro.Inject
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.util.system.GLUtil
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Shown where the device can draw textures larger than the size every device can. */
@Inject
class AndroidHardwareBitmapSetting(
    private val basePreferences: BasePreferences,
) : HardwareBitmapSetting {
    @Composable
    override fun preferenceItem(): Preference.PreferenceItem<out Any, out Any>? {
        if (ImageUtil.HARDWARE_BITMAP_UNSUPPORTED ||
            GLUtil.DEVICE_TEXTURE_LIMIT <= GLUtil.SAFE_TEXTURE_LIMIT
        ) {
            return null
        }
        return Preference.PreferenceItem.ListPreference(
            preference = basePreferences.hardwareBitmapThreshold,
            entries = GLUtil.CUSTOM_TEXTURE_LIMIT_OPTIONS
                .mapIndexed { index, option ->
                    val display = if (index == 0) {
                        stringResource(MR.strings.pref_hardware_bitmap_threshold_default, option)
                    } else {
                        option.toString()
                    }
                    option to display
                }
                .toMap(),
            title = stringResource(MR.strings.pref_hardware_bitmap_threshold),
            subtitleProvider = { value, options ->
                stringResource(MR.strings.pref_hardware_bitmap_threshold_summary, options[value].orEmpty())
            },
        )
    }
}
