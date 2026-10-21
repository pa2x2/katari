package eu.kanade.presentation.reader.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsScreenModel
import kotlinx.coroutines.launch
import mihon.entry.interactions.reader.settings.MangaReaderSettings.Companion.ColorFilterMode
import mihon.entry.viewer.settings.updateEntry
import mihon.presentation.kit.chip.FilterChip
import mihon.presentation.kit.list.ChipsRow
import mihon.presentation.kit.list.RowGroup
import mihon.presentation.kit.list.RowGroupGap
import mihon.presentation.kit.list.SliderRow
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

@Composable
internal fun ColumnScope.ColorFilterPage(screenModel: ReaderSettingsScreenModel) {
    val settings = screenModel.settings
    val scope = rememberCoroutineScope()

    val customBrightness by settings.customBrightness.state.collectAsState()
    val customBrightnessValue by settings.customBrightnessValue.state.collectAsState()
    val colorFilter by settings.colorFilter.state.collectAsState()
    val colorFilterValue by settings.colorFilterValue.state.collectAsState()
    val colorFilterMode by settings.colorFilterMode.state.collectAsState()

    RowGroup {
        row { BindingToggleRow(stringResource(MR.strings.pref_custom_brightness), settings.customBrightness) }
        /*
         * Sets the brightness of the screen. Range is [-75, 100].
         * From -75 to -1 a semi-transparent black view is shown at the top with the minimum brightness.
         * From 1 to 100 it sets that value as brightness.
         * 0 sets system brightness and hides the overlay.
         */
        if (customBrightness.effectiveValue) {
            row {
                SliderRow(
                    value = customBrightnessValue.effectiveValue,
                    valueRange = -75..100,
                    steps = 0,
                    title = stringResource(MR.strings.pref_custom_brightness),
                    onValueChange = { value -> scope.launch { settings.customBrightnessValue.updateEntry(value) } },
                )
            }
        }
        row { BindingToggleRow(stringResource(MR.strings.pref_custom_color_filter), settings.colorFilter) }
        if (colorFilter.effectiveValue) {
            row {
                ColorChannelSlider(
                    title = stringResource(MR.strings.color_filter_r_value),
                    value = colorFilterValue.effectiveValue.red,
                    onValueChange = { red ->
                        val newValue = getColorValue(colorFilterValue.effectiveValue, red, RED_MASK, 16)
                        scope.launch { settings.colorFilterValue.updateEntry(newValue) }
                    },
                )
            }
            row {
                ColorChannelSlider(
                    title = stringResource(MR.strings.color_filter_g_value),
                    value = colorFilterValue.effectiveValue.green,
                    onValueChange = { green ->
                        val newValue = getColorValue(colorFilterValue.effectiveValue, green, GREEN_MASK, 8)
                        scope.launch { settings.colorFilterValue.updateEntry(newValue) }
                    },
                )
            }
            row {
                ColorChannelSlider(
                    title = stringResource(MR.strings.color_filter_b_value),
                    value = colorFilterValue.effectiveValue.blue,
                    onValueChange = { blue ->
                        val newValue = getColorValue(colorFilterValue.effectiveValue, blue, BLUE_MASK, 0)
                        scope.launch { settings.colorFilterValue.updateEntry(newValue) }
                    },
                )
            }
            row {
                ColorChannelSlider(
                    title = stringResource(MR.strings.color_filter_a_value),
                    value = colorFilterValue.effectiveValue.alpha,
                    onValueChange = { alpha ->
                        val newValue = getColorValue(colorFilterValue.effectiveValue, alpha, ALPHA_MASK, 24)
                        scope.launch { settings.colorFilterValue.updateEntry(newValue) }
                    },
                )
            }
        }
    }

    if (colorFilter.effectiveValue) {
        ChipsRow(title = stringResource(MR.strings.pref_color_filter_mode)) {
            ColorFilterMode.mapIndexed { index, it ->
                FilterChip(
                    selected = colorFilterMode.effectiveValue == index,
                    onClick = { scope.launch { settings.colorFilterMode.updateEntry(index) } },
                    label = stringResource(it.first),
                )
            }
        }
    } else {
        RowGroupGap()
    }

    RowGroup {
        row { BindingToggleRow(stringResource(MR.strings.pref_grayscale), settings.grayscale) }
        row { BindingToggleRow(stringResource(MR.strings.pref_inverted_colors), settings.invertedColors) }
    }
}

@Composable
private fun ColorChannelSlider(title: String, value: Int, onValueChange: (Int) -> Unit) {
    SliderRow(value = value, valueRange = 0..255, steps = 0, title = title, onValueChange = onValueChange)
}

private fun getColorValue(currentColor: Int, color: Int, mask: Long, bitShift: Int): Int {
    return (color shl bitShift) or (currentColor and mask.inv().toInt())
}
private const val ALPHA_MASK: Long = 0xFF000000
private const val RED_MASK: Long = 0x00FF0000
private const val GREEN_MASK: Long = 0x0000FF00
private const val BLUE_MASK: Long = 0x000000FF
