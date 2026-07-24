package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import eu.kanade.presentation.more.settings.Preference
import mihon.entry.interactions.settings.ReadiumEpubSettingsProvider
import mihon.entry.viewer.settings.ViewerSettingBinder
import mihon.entry.viewer.settings.asProfilePreference
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsReadiumEpubReaderScreen : AppEntryViewerSettingsScreenProjection {

    override val surfaceId: String = ReadiumEpubSettingsProvider.PROVIDER_ID

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_epub_readium_reader

    @Composable
    override fun getPreferences(): List<Preference> {
        val provider = remember { Injekt.get<ReadiumEpubSettingsProvider>() }
        val binder = remember { Injekt.get<ViewerSettingBinder>() }
        val theme = remember(provider, binder) { binder.bind(provider.themeSetting).asProfilePreference() }
        val fontFamily = remember(provider, binder) { binder.bind(provider.fontFamilySetting).asProfilePreference() }
        val fontSize = remember(provider, binder) { binder.bind(provider.fontSizeSetting).asProfilePreference() }
        val lineHeight = remember(provider, binder) { binder.bind(provider.lineHeightSetting).asProfilePreference() }
        val pageMargins = remember(provider, binder) { binder.bind(provider.pageMarginsSetting).asProfilePreference() }
        val publisherStyles = remember(provider, binder) {
            binder.bind(provider.publisherStylesSetting).asProfilePreference()
        }
        val textAlignment = remember(provider, binder) {
            binder.bind(provider.textAlignmentSetting).asProfilePreference()
        }
        val layoutMode = remember(provider, binder) { binder.bind(provider.layoutModeSetting).asProfilePreference() }
        val columnCount = remember(provider, binder) { binder.bind(provider.columnCountSetting).asProfilePreference() }
        val textNormalization = remember(provider, binder) {
            binder.bind(provider.textNormalizationSetting).asProfilePreference()
        }
        val tapNavigation = remember(provider, binder) {
            binder.bind(provider.tapNavigationSetting).asProfilePreference()
        }
        val showPageNumber = remember(provider, binder) {
            binder.bind(provider.showPageNumberSetting).asProfilePreference()
        }

        val fontSizeValue by fontSize.collectAsState()
        val lineHeightValue by lineHeight.collectAsState()
        val pageMarginsValue by pageMargins.collectAsState()
        val publisherStylesEnabled by publisherStyles.collectAsState()
        val layoutModeValue by layoutMode.collectAsState()

        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_category_display),
                preferenceItems = listOf(
                    Preference.PreferenceItem.ListPreference(
                        preference = theme,
                        entries = mapOf(
                            ReadiumEpubSettingsProvider.THEME_LIGHT to
                                stringResource(MR.strings.pref_epub_theme_light),
                            ReadiumEpubSettingsProvider.THEME_DARK to
                                stringResource(MR.strings.pref_epub_theme_dark),
                            ReadiumEpubSettingsProvider.THEME_SEPIA to
                                stringResource(MR.strings.pref_epub_theme_sepia),
                        ),
                        title = stringResource(MR.strings.pref_epub_color_theme),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        preference = fontFamily,
                        entries = mapOf(
                            ReadiumEpubSettingsProvider.FONT_PUBLISHER to
                                stringResource(MR.strings.pref_epub_font_publisher),
                            ReadiumEpubSettingsProvider.FONT_SERIF to
                                stringResource(MR.strings.pref_epub_font_serif),
                            ReadiumEpubSettingsProvider.FONT_SANS_SERIF to
                                stringResource(MR.strings.pref_epub_font_sans_serif),
                            ReadiumEpubSettingsProvider.FONT_MONOSPACE to
                                stringResource(MR.strings.pref_epub_font_monospace),
                            ReadiumEpubSettingsProvider.FONT_OPEN_DYSLEXIC to "OpenDyslexic",
                        ),
                        title = stringResource(MR.strings.pref_epub_font_family),
                    ),
                    Preference.PreferenceItem.SliderPreference(
                        value = fontSizeValue,
                        preference = fontSize,
                        valueRange = ReadiumEpubSettingsProvider.FONT_SIZE_RANGE,
                        steps = 24,
                        title = stringResource(MR.strings.pref_epub_font_size),
                        valueString = "$fontSizeValue%",
                        onValueChanged = fontSize::set,
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_epub_page_layout),
                preferenceItems = listOf(
                    Preference.PreferenceItem.ListPreference(
                        preference = layoutMode,
                        entries = mapOf(
                            ReadiumEpubSettingsProvider.LAYOUT_PAGINATED to
                                stringResource(MR.strings.pref_epub_layout_paginated),
                            ReadiumEpubSettingsProvider.LAYOUT_SCROLLING to
                                stringResource(MR.strings.pref_epub_layout_scrolling),
                        ),
                        title = stringResource(MR.strings.pref_epub_layout_mode),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        preference = columnCount,
                        entries = mapOf(
                            ReadiumEpubSettingsProvider.COLUMNS_AUTO to
                                stringResource(MR.strings.pref_epub_columns_auto),
                            ReadiumEpubSettingsProvider.COLUMNS_ONE to
                                stringResource(MR.strings.pref_epub_columns_one),
                            ReadiumEpubSettingsProvider.COLUMNS_TWO to
                                stringResource(MR.strings.pref_epub_columns_two),
                        ),
                        title = stringResource(MR.strings.pref_epub_column_count),
                        subtitle = stringResource(MR.strings.pref_epub_column_count_summary),
                        enabled = layoutModeValue == ReadiumEpubSettingsProvider.LAYOUT_PAGINATED,
                    ),
                    Preference.PreferenceItem.SliderPreference(
                        value = pageMarginsValue,
                        preference = pageMargins,
                        valueRange = ReadiumEpubSettingsProvider.PAGE_MARGINS_RANGE,
                        steps = 19,
                        title = stringResource(MR.strings.pref_epub_page_margins),
                        valueString = "$pageMarginsValue%",
                        onValueChanged = pageMargins::set,
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_epub_text_layout),
                preferenceItems = listOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = publisherStyles,
                        title = stringResource(MR.strings.pref_epub_publisher_styles),
                        subtitle = stringResource(MR.strings.pref_epub_publisher_styles_summary),
                    ),
                    Preference.PreferenceItem.SliderPreference(
                        value = lineHeightValue,
                        preference = lineHeight,
                        valueRange = ReadiumEpubSettingsProvider.LINE_HEIGHT_RANGE,
                        steps = 9,
                        title = stringResource(MR.strings.pref_epub_line_height),
                        valueString = "$lineHeightValue%",
                        enabled = !publisherStylesEnabled,
                        onValueChanged = lineHeight::set,
                    ),
                    Preference.PreferenceItem.ListPreference(
                        preference = textAlignment,
                        entries = mapOf(
                            ReadiumEpubSettingsProvider.ALIGN_PUBLISHER to
                                stringResource(MR.strings.pref_epub_alignment_publisher),
                            ReadiumEpubSettingsProvider.ALIGN_START to
                                stringResource(MR.strings.pref_epub_alignment_start),
                            ReadiumEpubSettingsProvider.ALIGN_JUSTIFY to
                                stringResource(MR.strings.pref_epub_alignment_justify),
                            ReadiumEpubSettingsProvider.ALIGN_LEFT to
                                stringResource(MR.strings.pref_epub_alignment_left),
                            ReadiumEpubSettingsProvider.ALIGN_RIGHT to
                                stringResource(MR.strings.pref_epub_alignment_right),
                        ),
                        title = stringResource(MR.strings.pref_epub_text_alignment),
                        enabled = !publisherStylesEnabled,
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = textNormalization,
                        title = stringResource(MR.strings.pref_epub_text_normalization),
                        subtitle = stringResource(MR.strings.pref_epub_text_normalization_summary),
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_epub_controls),
                preferenceItems = listOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = tapNavigation,
                        title = stringResource(MR.strings.pref_epub_tap_navigation),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = showPageNumber,
                        title = stringResource(MR.strings.pref_epub_show_reading_progress),
                    ),
                ),
            ),
        )
    }
}
