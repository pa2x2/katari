package eu.kanade.presentation.reader.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.SettingsChipRow
import tachiyomi.presentation.core.components.SliderItem
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

@Composable
internal fun ColumnScope.ColorFilterPage(screenModel: ReaderSettingsScreenModel) {
    val settings = screenModel.settings
    val scope = rememberCoroutineScope()

    val customBrightness by settings.customBrightness.state.collectAsState()
    BindingCheckboxItem(
        label = stringResource(MR.strings.pref_custom_brightness),
        binding = settings.customBrightness,
    )

    /*
     * Sets the brightness of the screen. Range is [-75, 100].
     * From -75 to -1 a semi-transparent black view is shown at the top with the minimum brightness.
     * From 1 to 100 it sets that value as brightness.
     * 0 sets system brightness and hides the overlay.
     */
    if (customBrightness.effectiveValue) {
        val customBrightnessValue by settings.customBrightnessValue.state.collectAsState()
        SliderItem(
            value = customBrightnessValue.effectiveValue,
            valueRange = -75..100,
            steps = 0,
            label = stringResource(MR.strings.pref_custom_brightness),
            onChange = { value -> scope.launch { settings.customBrightnessValue.updateEntry(value) } },
            pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }

    val colorFilter by settings.colorFilter.state.collectAsState()
    BindingCheckboxItem(
        label = stringResource(MR.strings.pref_custom_color_filter),
        binding = settings.colorFilter,
    )
    if (colorFilter.effectiveValue) {
        val colorFilterValue by settings.colorFilterValue.state.collectAsState()
        SliderItem(
            value = colorFilterValue.effectiveValue.red,
            valueRange = 0..255,
            steps = 0,
            label = stringResource(MR.strings.color_filter_r_value),
            onChange = { newRValue ->
                val newValue = getColorValue(colorFilterValue.effectiveValue, newRValue, RED_MASK, 16)
                scope.launch { settings.colorFilterValue.updateEntry(newValue) }
            },
            pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
        SliderItem(
            value = colorFilterValue.effectiveValue.green,
            valueRange = 0..255,
            steps = 0,
            label = stringResource(MR.strings.color_filter_g_value),
            onChange = { newGValue ->
                val newValue = getColorValue(colorFilterValue.effectiveValue, newGValue, GREEN_MASK, 8)
                scope.launch { settings.colorFilterValue.updateEntry(newValue) }
            },
            pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
        SliderItem(
            value = colorFilterValue.effectiveValue.blue,
            valueRange = 0..255,
            steps = 0,
            label = stringResource(MR.strings.color_filter_b_value),
            onChange = { newBValue ->
                val newValue = getColorValue(colorFilterValue.effectiveValue, newBValue, BLUE_MASK, 0)
                scope.launch { settings.colorFilterValue.updateEntry(newValue) }
            },
            pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
        SliderItem(
            value = colorFilterValue.effectiveValue.alpha,
            valueRange = 0..255,
            steps = 0,
            label = stringResource(MR.strings.color_filter_a_value),
            onChange = { newAValue ->
                val newValue = getColorValue(colorFilterValue.effectiveValue, newAValue, ALPHA_MASK, 24)
                scope.launch { settings.colorFilterValue.updateEntry(newValue) }
            },
            pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )

        val colorFilterMode by settings.colorFilterMode.state.collectAsState()
        SettingsChipRow(MR.strings.pref_color_filter_mode) {
            ColorFilterMode.mapIndexed { index, it ->
                FilterChip(
                    selected = colorFilterMode.effectiveValue == index,
                    onClick = { scope.launch { settings.colorFilterMode.updateEntry(index) } },
                    label = { Text(stringResource(it.first)) },
                )
            }
        }
    }

    BindingCheckboxItem(
        label = stringResource(MR.strings.pref_grayscale),
        binding = settings.grayscale,
    )
    BindingCheckboxItem(
        label = stringResource(MR.strings.pref_inverted_colors),
        binding = settings.invertedColors,
    )
}

private fun getColorValue(currentColor: Int, color: Int, mask: Long, bitShift: Int): Int {
    return (color shl bitShift) or (currentColor and mask.inv().toInt())
}
private const val ALPHA_MASK: Long = 0xFF000000
private const val RED_MASK: Long = 0x00FF0000
private const val GREEN_MASK: Long = 0x0000FF00
private const val BLUE_MASK: Long = 0x000000FF
