package eu.kanade.presentation.more.settings.screen.advanced

import androidx.compose.runtime.Composable
import dev.zacsweers.metro.Inject
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.util.system.GLUtil
import mihon.core.image.progressive.ProgressiveImageEngine
import mihon.core.image.progressive.ProgressiveImagePreferences
import mihon.presentation.kit.files.DocumentAccess
import mihon.presentation.kit.files.DocumentPickerRequest
import mihon.presentation.kit.files.rememberDocumentPicker
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The manga reader's progressive loading, hardware bitmaps and colour profile. */
@Inject
class AndroidReaderDecodingSettings(
    private val basePreferences: BasePreferences,
    private val progressiveImagePreferences: ProgressiveImagePreferences,
    private val progressiveImageEngine: ProgressiveImageEngine,
) : ReaderDecodingSettings {
    override val isOffered: Boolean = true

    @Composable
    override fun preferenceItems(): List<Preference.PreferenceItem<out Any, out Any>> {
        val chooseColorProfile = rememberDocumentPicker(
            DocumentPickerRequest.OpenDocument(listOf("*/*"), keepAccess = DocumentAccess.Read),
        ) { pick ->
            pick?.let { basePreferences.displayProfile.set(it.uri.value) }
        }
        return listOf(
            Preference.PreferenceItem.SwitchPreference(
                preference = progressiveImagePreferences.enabled,
                title = stringResource(MR.strings.pref_progressive_image_loading),
                subtitle = stringResource(MR.strings.pref_progressive_image_loading_summary),
                onValueChanged = { enabled ->
                    if (!enabled) progressiveImageEngine.disableActiveSessions()
                    true
                },
            ),
            Preference.PreferenceItem.ListPreference(
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
                visible = !ImageUtil.HARDWARE_BITMAP_UNSUPPORTED &&
                    GLUtil.DEVICE_TEXTURE_LIMIT > GLUtil.SAFE_TEXTURE_LIMIT,
            ),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(MR.strings.pref_display_profile),
                subtitle = basePreferences.displayProfile.get(),
                onClick = {
                    chooseColorProfile.launch()
                },
            ),
        )
    }
}
