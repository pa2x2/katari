package mihon.entry.interactions.book.prose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mihon.entry.interactions.book.BookReaderSettingsDialog
import mihon.entry.interactions.book.R
import mihon.entry.interactions.settings.HtmlProseSettingsProvider
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.SettingsItemsPaddings
import tachiyomi.presentation.core.components.SliderItem
import tachiyomi.presentation.core.i18n.stringResource as i18nStringResource

@Composable
internal fun HtmlProseSettingsDialog(
    settings: HtmlProseSettingsBinding,
    onDismissRequest: () -> Unit,
) {
    val tabs = listOf(
        i18nStringResource(MR.strings.pref_category_display),
        i18nStringResource(MR.strings.pref_epub_page_layout),
        i18nStringResource(MR.strings.pref_epub_controls),
    )
    BookReaderSettingsDialog(
        settingsSurfaceId = settings.readerSettingsSurfaceId,
        capabilities = settings.readerCapabilities,
        onDismissRequest = onDismissRequest,
        onResetProcessorSettings = settings::resetSettings,
        processorTabTitles = tabs,
    ) { page ->
        when (page) {
            0 -> ProseAppearanceSettings(settings)
            1 -> ProseLayoutSettings(settings)
            2 -> ProseControlSettings(settings)
        }
    }
}

@Composable
internal fun ProseAppearanceSettings(settings: HtmlProseSettingsBinding) {
    val theme by settings.theme.state.collectEffectiveValue()
    val font by settings.fontFamily.state.collectEffectiveValue()
    val fontSize by settings.fontSize.state.collectEffectiveValue()
    val drawUnderCutout by settings.drawUnderCutout.state.collectEffectiveValue()
    ProseSettingChips(
        stringResource(R.string.prose_reader_theme),
        listOf(
            HtmlProseSettingsProvider.THEME_SYSTEM to stringResource(R.string.prose_reader_theme_system),
            HtmlProseSettingsProvider.THEME_LIGHT to stringResource(R.string.prose_reader_theme_light),
            HtmlProseSettingsProvider.THEME_SEPIA to stringResource(R.string.prose_reader_theme_sepia),
            HtmlProseSettingsProvider.THEME_DARK to stringResource(R.string.prose_reader_theme_dark),
            HtmlProseSettingsProvider.THEME_BLACK to stringResource(R.string.prose_reader_theme_black),
        ),
        theme,
        settings.theme::setProfileValue,
    )
    ProseSettingChips(
        stringResource(R.string.prose_reader_font),
        listOf(
            HtmlProseSettingsProvider.FONT_SERIF to stringResource(R.string.prose_reader_font_serif),
            HtmlProseSettingsProvider.FONT_SANS_SERIF to stringResource(R.string.prose_reader_font_sans_serif),
            HtmlProseSettingsProvider.FONT_MONOSPACE to stringResource(R.string.prose_reader_font_monospace),
        ),
        font,
        settings.fontFamily::setProfileValue,
    )
    SliderItem(
        value = fontSize,
        valueRange = HtmlProseSettingsProvider.FONT_SIZE_RANGE step 10,
        label = stringResource(R.string.prose_reader_font_size),
        valueString = "$fontSize%",
        onChange = settings.fontSize::setProfileValue,
    )
    CheckboxItem(
        label = i18nStringResource(MR.strings.pref_cutout_short),
        checked = drawUnderCutout,
        onClick = { settings.drawUnderCutout.setProfileValue(!drawUnderCutout) },
    )
}

@Composable
internal fun ProseLayoutSettings(settings: HtmlProseSettingsBinding) {
    val scope = rememberCoroutineScope()
    val layout by settings.layoutMode.state.collectEffectiveValue()
    val lineHeight by settings.lineHeight.state.collectEffectiveValue()
    val margins by settings.pageMargins.state.collectEffectiveValue()
    val alignment by settings.textAlignment.state.collectEffectiveValue()
    ProseSettingChips(
        stringResource(R.string.prose_reader_layout),
        listOf(
            HtmlProseSettingsProvider.LAYOUT_PAGINATED to stringResource(R.string.prose_reader_layout_paginated),
            HtmlProseSettingsProvider.LAYOUT_SCROLLING to stringResource(R.string.prose_reader_layout_scrolling),
        ),
        layout,
        { scope.launch { settings.layoutMode.setEntryOverride(it) } },
    )
    SliderItem(
        lineHeight,
        HtmlProseSettingsProvider.LINE_HEIGHT_RANGE step 10,
        stringResource(R.string.prose_reader_line_height),
        settings.lineHeight::setProfileValue,
        valueString = "$lineHeight%",
    )
    SliderItem(
        margins,
        HtmlProseSettingsProvider.PAGE_MARGINS_RANGE step 10,
        stringResource(R.string.prose_reader_page_margins),
        settings.pageMargins::setProfileValue,
        valueString = "$margins%",
    )
    ProseSettingChips(
        stringResource(R.string.prose_reader_text_alignment),
        listOf(
            HtmlProseSettingsProvider.ALIGN_START to stringResource(R.string.prose_reader_alignment_start),
            HtmlProseSettingsProvider.ALIGN_JUSTIFY to stringResource(R.string.prose_reader_alignment_justify),
            HtmlProseSettingsProvider.ALIGN_LEFT to stringResource(R.string.prose_reader_alignment_left),
            HtmlProseSettingsProvider.ALIGN_RIGHT to stringResource(R.string.prose_reader_alignment_right),
        ),
        alignment,
        settings.textAlignment::setProfileValue,
    )
}

@Composable
internal fun ProseControlSettings(settings: HtmlProseSettingsBinding) {
    val layout by settings.layoutMode.state.collectEffectiveValue()
    val tapNavigation by settings.tapNavigation.state.collectEffectiveValue()
    val showProgress by settings.showProgress.state.collectEffectiveValue()
    if (layout == HtmlProseSettingsProvider.LAYOUT_PAGINATED) {
        CheckboxItem(
            label = stringResource(R.string.prose_reader_tap_navigation),
            checked = tapNavigation,
            onClick = { settings.tapNavigation.setProfileValue(!tapNavigation) },
        )
    }
    CheckboxItem(
        label = stringResource(R.string.prose_reader_show_progress),
        checked = showProgress,
        onClick = { settings.showProgress.setProfileValue(!showProgress) },
    )
}

@Composable
internal fun ProseSettingChips(
    label: String,
    values: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column {
        HeadingItem(label)
        FlowRow(
            modifier = Modifier.padding(
                start = SettingsItemsPaddings.Horizontal,
                end = SettingsItemsPaddings.Horizontal,
                bottom = SettingsItemsPaddings.Vertical,
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            values.forEach { (value, text) ->
                FilterChip(selected == value, { onSelect(value) }, label = { Text(text) })
            }
        }
    }
}
