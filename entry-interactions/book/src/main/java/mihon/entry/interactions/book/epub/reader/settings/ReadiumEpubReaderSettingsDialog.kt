package mihon.entry.interactions.book.epub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import mihon.entry.interactions.book.BookReaderSettingsDialog
import mihon.entry.interactions.settings.ReadiumEpubSettingsProvider
import mihon.entry.viewer.settings.ResolvedViewerSetting
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.SettingsItemsPaddings
import tachiyomi.presentation.core.components.SliderItem
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun ReadiumEpubSettingsDialog(
    settings: ReadiumEpubSettingsBinding,
    onDismissRequest: () -> Unit,
) {
    val tabs = listOf(
        stringResource(MR.strings.pref_category_display),
        stringResource(MR.strings.pref_epub_page_layout),
        stringResource(MR.strings.pref_epub_controls),
    )
    BookReaderSettingsDialog(
        settingsSurfaceId = settings.readerSettingsSurfaceId,
        capabilities = settings.readerCapabilities,
        onDismissRequest = onDismissRequest,
        onResetProcessorSettings = settings::resetSettings,
        processorTabTitles = tabs,
    ) { page ->
        when (page) {
            0 -> ReadiumAppearanceSettings(settings)
            1 -> ReadiumLayoutSettings(settings)
            2 -> ReadiumControlSettings(settings)
        }
    }
}

@Composable
internal fun ReadiumAppearanceSettings(settings: ReadiumEpubSettingsBinding) {
    val theme by settings.theme.state.collectEffectiveValue()
    val fontFamily by settings.fontFamily.state.collectEffectiveValue()
    val fontSize by settings.fontSize.state.collectEffectiveValue()

    SettingChips(
        label = stringResource(MR.strings.pref_epub_color_theme),
        values = listOf(
            ReadiumEpubSettingsProvider.THEME_LIGHT to stringResource(MR.strings.pref_epub_theme_light),
            ReadiumEpubSettingsProvider.THEME_DARK to stringResource(MR.strings.pref_epub_theme_dark),
            ReadiumEpubSettingsProvider.THEME_SEPIA to stringResource(MR.strings.pref_epub_theme_sepia),
        ),
        selected = theme,
        onSelect = settings.theme::setProfileValue,
    )
    SettingChips(
        label = stringResource(MR.strings.pref_epub_font_family),
        values = listOf(
            ReadiumEpubSettingsProvider.FONT_PUBLISHER to stringResource(MR.strings.pref_epub_font_publisher),
            ReadiumEpubSettingsProvider.FONT_SERIF to stringResource(MR.strings.pref_epub_font_serif),
            ReadiumEpubSettingsProvider.FONT_SANS_SERIF to stringResource(MR.strings.pref_epub_font_sans_serif),
            ReadiumEpubSettingsProvider.FONT_MONOSPACE to stringResource(MR.strings.pref_epub_font_monospace),
            ReadiumEpubSettingsProvider.FONT_OPEN_DYSLEXIC to "OpenDyslexic",
        ),
        selected = fontFamily,
        onSelect = settings.fontFamily::setProfileValue,
    )
    SliderItem(
        value = fontSize,
        valueRange = ReadiumEpubSettingsProvider.FONT_SIZE_RANGE step 10,
        steps = 24,
        label = stringResource(MR.strings.pref_epub_font_size),
        valueString = "$fontSize%",
        onChange = settings.fontSize::setProfileValue,
    )
}

@Composable
internal fun ReadiumLayoutSettings(settings: ReadiumEpubSettingsBinding) {
    val columnCount by settings.columnCount.state.collectEffectiveValue()
    val pageMargins by settings.pageMargins.state.collectEffectiveValue()
    val publisherStyles by settings.publisherStyles.state.collectEffectiveValue()
    val lineHeight by settings.lineHeight.state.collectEffectiveValue()
    val textAlignment by settings.textAlignment.state.collectEffectiveValue()
    val textNormalization by settings.textNormalization.state.collectEffectiveValue()

    SettingChips(
        label = stringResource(MR.strings.pref_epub_column_count),
        summary = stringResource(MR.strings.pref_epub_column_count_summary),
        values = listOf(
            ReadiumEpubSettingsProvider.COLUMNS_AUTO to stringResource(MR.strings.pref_epub_columns_auto),
            ReadiumEpubSettingsProvider.COLUMNS_ONE to stringResource(MR.strings.pref_epub_columns_one),
            ReadiumEpubSettingsProvider.COLUMNS_TWO to stringResource(MR.strings.pref_epub_columns_two),
        ),
        selected = columnCount,
        onSelect = settings.columnCount::setProfileValue,
    )
    SliderItem(
        value = pageMargins,
        valueRange = ReadiumEpubSettingsProvider.PAGE_MARGINS_RANGE step 20,
        steps = 19,
        label = stringResource(MR.strings.pref_epub_page_margins),
        valueString = "$pageMargins%",
        onChange = settings.pageMargins::setProfileValue,
    )
    CheckboxItem(
        label = stringResource(MR.strings.pref_epub_publisher_styles),
        checked = publisherStyles,
        onClick = { settings.publisherStyles.setProfileValue(!publisherStyles) },
    )
    if (!publisherStyles) {
        SliderItem(
            value = lineHeight,
            valueRange = ReadiumEpubSettingsProvider.LINE_HEIGHT_RANGE step 10,
            steps = 9,
            label = stringResource(MR.strings.pref_epub_line_height),
            valueString = "$lineHeight%",
            onChange = settings.lineHeight::setProfileValue,
        )
        SettingChips(
            label = stringResource(MR.strings.pref_epub_text_alignment),
            values = listOf(
                ReadiumEpubSettingsProvider.ALIGN_PUBLISHER to
                    stringResource(MR.strings.pref_epub_alignment_publisher),
                ReadiumEpubSettingsProvider.ALIGN_START to stringResource(MR.strings.pref_epub_alignment_start),
                ReadiumEpubSettingsProvider.ALIGN_JUSTIFY to stringResource(MR.strings.pref_epub_alignment_justify),
                ReadiumEpubSettingsProvider.ALIGN_LEFT to stringResource(MR.strings.pref_epub_alignment_left),
                ReadiumEpubSettingsProvider.ALIGN_RIGHT to stringResource(MR.strings.pref_epub_alignment_right),
            ),
            selected = textAlignment,
            onSelect = settings.textAlignment::setProfileValue,
        )
    }
    CheckboxItem(
        label = stringResource(MR.strings.pref_epub_text_normalization),
        subtitle = stringResource(MR.strings.pref_epub_text_normalization_summary),
        checked = textNormalization,
        onClick = { settings.textNormalization.setProfileValue(!textNormalization) },
    )
}

@Composable
internal fun ReadiumControlSettings(settings: ReadiumEpubSettingsBinding) {
    val tapNavigation by settings.tapNavigation.state.collectEffectiveValue()
    val showPageNumber by settings.showPageNumber.state.collectEffectiveValue()
    CheckboxItem(
        label = stringResource(MR.strings.pref_epub_tap_navigation),
        checked = tapNavigation,
        onClick = { settings.tapNavigation.setProfileValue(!tapNavigation) },
    )
    CheckboxItem(
        label = stringResource(MR.strings.pref_epub_show_reading_progress),
        checked = showPageNumber,
        onClick = { settings.showPageNumber.setProfileValue(!showPageNumber) },
    )
}

@Composable
internal fun SettingChips(
    label: String,
    summary: String? = null,
    values: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column {
        HeadingItem(label)
        summary?.let {
            Text(
                text = it,
                modifier = Modifier.padding(
                    start = SettingsItemsPaddings.Horizontal,
                    end = SettingsItemsPaddings.Horizontal,
                    bottom = SettingsItemsPaddings.Vertical,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        FlowRow(
            modifier = Modifier.padding(
                start = SettingsItemsPaddings.Horizontal,
                end = SettingsItemsPaddings.Horizontal,
                bottom = SettingsItemsPaddings.Vertical,
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            values.forEach { (value, text) ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    label = { Text(text) },
                )
            }
        }
    }
}

@Composable
internal fun <T> StateFlow<ResolvedViewerSetting<T>>.collectEffectiveValue(): State<T> {
    val resolved by collectAsState()
    return rememberUpdatedState(resolved.effectiveValue)
}
