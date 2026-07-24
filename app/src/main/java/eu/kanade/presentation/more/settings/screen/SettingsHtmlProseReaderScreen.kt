package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import eu.kanade.presentation.more.settings.Preference
import mihon.entry.interactions.settings.HtmlProseSettingsProvider
import mihon.entry.viewer.settings.ViewerSettingBinder
import mihon.entry.viewer.settings.asProfilePreference
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsHtmlProseReaderScreen : AppEntryViewerSettingsScreenProjection {

    override val surfaceId: String = HtmlProseSettingsProvider.PROVIDER_ID

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_web_prose_reader

    @Composable
    override fun getPreferences(): List<Preference> {
        val provider = remember { Injekt.get<HtmlProseSettingsProvider>() }
        val binder = remember { Injekt.get<ViewerSettingBinder>() }
        val theme = remember(provider, binder) { binder.bind(provider.themeSetting).asProfilePreference() }
        val fontFamily = remember(provider, binder) { binder.bind(provider.fontFamilySetting).asProfilePreference() }
        val fontSize = remember(provider, binder) { binder.bind(provider.fontSizeSetting).asProfilePreference() }
        val lineHeight = remember(provider, binder) { binder.bind(provider.lineHeightSetting).asProfilePreference() }
        val pageMargins = remember(provider, binder) { binder.bind(provider.pageMarginsSetting).asProfilePreference() }
        val textAlignment = remember(provider, binder) {
            binder.bind(provider.textAlignmentSetting).asProfilePreference()
        }
        val layoutMode = remember(provider, binder) { binder.bind(provider.layoutModeSetting).asProfilePreference() }
        val tapNavigation = remember(provider, binder) {
            binder.bind(provider.tapNavigationSetting).asProfilePreference()
        }
        val showProgress = remember(provider, binder) {
            binder.bind(provider.showProgressSetting).asProfilePreference()
        }
        val drawUnderCutout = remember(provider, binder) {
            binder.bind(provider.drawUnderCutoutSetting).asProfilePreference()
        }

        val fontSizeValue by fontSize.collectAsState()
        val lineHeightValue by lineHeight.collectAsState()
        val pageMarginsValue by pageMargins.collectAsState()
        val layoutModeValue by layoutMode.collectAsState()

        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_category_display),
                preferenceItems = listOf(
                    Preference.PreferenceItem.ListPreference(
                        preference = theme,
                        entries = mapOf(
                            HtmlProseSettingsProvider.THEME_SYSTEM to
                                stringResource(MR.strings.pref_prose_theme_system),
                            HtmlProseSettingsProvider.THEME_LIGHT to
                                stringResource(MR.strings.pref_epub_theme_light),
                            HtmlProseSettingsProvider.THEME_SEPIA to
                                stringResource(MR.strings.pref_epub_theme_sepia),
                            HtmlProseSettingsProvider.THEME_DARK to
                                stringResource(MR.strings.pref_epub_theme_dark),
                            HtmlProseSettingsProvider.THEME_BLACK to
                                stringResource(MR.strings.pref_prose_theme_black),
                        ),
                        title = stringResource(MR.strings.pref_epub_color_theme),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        preference = fontFamily,
                        entries = mapOf(
                            HtmlProseSettingsProvider.FONT_SERIF to
                                stringResource(MR.strings.pref_epub_font_serif),
                            HtmlProseSettingsProvider.FONT_SANS_SERIF to
                                stringResource(MR.strings.pref_epub_font_sans_serif),
                            HtmlProseSettingsProvider.FONT_MONOSPACE to
                                stringResource(MR.strings.pref_epub_font_monospace),
                        ),
                        title = stringResource(MR.strings.pref_epub_font_family),
                    ),
                    Preference.PreferenceItem.SliderPreference(
                        value = fontSizeValue,
                        preference = fontSize,
                        valueRange = HtmlProseSettingsProvider.FONT_SIZE_RANGE step 10,
                        title = stringResource(MR.strings.pref_epub_font_size),
                        valueString = "$fontSizeValue%",
                        onValueChanged = fontSize::set,
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = drawUnderCutout,
                        title = stringResource(MR.strings.pref_cutout_short),
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_epub_page_layout),
                preferenceItems = listOf(
                    Preference.PreferenceItem.ListPreference(
                        preference = layoutMode,
                        entries = mapOf(
                            HtmlProseSettingsProvider.LAYOUT_PAGINATED to
                                stringResource(MR.strings.pref_epub_layout_paginated),
                            HtmlProseSettingsProvider.LAYOUT_SCROLLING to
                                stringResource(MR.strings.pref_epub_layout_scrolling),
                        ),
                        title = stringResource(MR.strings.pref_epub_layout_mode),
                    ),
                    Preference.PreferenceItem.SliderPreference(
                        value = pageMarginsValue,
                        preference = pageMargins,
                        valueRange = HtmlProseSettingsProvider.PAGE_MARGINS_RANGE step 10,
                        title = stringResource(MR.strings.pref_epub_page_margins),
                        valueString = "$pageMarginsValue%",
                        onValueChanged = pageMargins::set,
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_epub_text_layout),
                preferenceItems = listOf(
                    Preference.PreferenceItem.SliderPreference(
                        value = lineHeightValue,
                        preference = lineHeight,
                        valueRange = HtmlProseSettingsProvider.LINE_HEIGHT_RANGE step 10,
                        title = stringResource(MR.strings.pref_epub_line_height),
                        valueString = "$lineHeightValue%",
                        onValueChanged = lineHeight::set,
                    ),
                    Preference.PreferenceItem.ListPreference(
                        preference = textAlignment,
                        entries = mapOf(
                            HtmlProseSettingsProvider.ALIGN_START to
                                stringResource(MR.strings.pref_epub_alignment_start),
                            HtmlProseSettingsProvider.ALIGN_JUSTIFY to
                                stringResource(MR.strings.pref_epub_alignment_justify),
                            HtmlProseSettingsProvider.ALIGN_LEFT to
                                stringResource(MR.strings.pref_epub_alignment_left),
                            HtmlProseSettingsProvider.ALIGN_RIGHT to
                                stringResource(MR.strings.pref_epub_alignment_right),
                        ),
                        title = stringResource(MR.strings.pref_epub_text_alignment),
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_epub_controls),
                preferenceItems = listOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = tapNavigation,
                        title = stringResource(MR.strings.pref_epub_tap_navigation),
                        enabled = layoutModeValue == HtmlProseSettingsProvider.LAYOUT_PAGINATED,
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = showProgress,
                        title = stringResource(MR.strings.pref_epub_show_reading_progress),
                    ),
                ),
            ),
        )
    }
}
