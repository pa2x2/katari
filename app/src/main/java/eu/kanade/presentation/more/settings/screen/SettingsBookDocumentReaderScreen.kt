package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import eu.kanade.presentation.more.settings.Preference
import mihon.entry.interactions.reader.settings.BookDocumentReaderProgressStyle
import mihon.entry.interactions.reader.settings.BookDocumentReaderSettings
import mihon.entry.interactions.reader.settings.BookDocumentReaderThemeMode
import mihon.entry.viewer.settings.ViewerSettingBinder
import mihon.entry.viewer.settings.asProfilePreference
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsBookDocumentReaderScreen : AppEntryViewerSettingsScreenProjection() {
    override val surfaceId = BookDocumentReaderSettings.SURFACE_ID

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_book_document_reader

    @Composable
    override fun getSurfacePreferences(): List<Preference> {
        val provider = remember { Injekt.get<BookDocumentReaderSettings>() }
        val binder = remember { Injekt.get<ViewerSettingBinder>() }
        val theme = remember(provider, binder) {
            binder.bind(provider.themeModeSetting).asProfilePreference()
        }
        val textSize = remember(provider, binder) {
            binder.bind(provider.textSizeSetting).asProfilePreference()
        }
        val showStatusBar = remember(provider, binder) {
            binder.bind(provider.showStatusBarSetting).asProfilePreference()
        }
        val showNavigationBar = remember(provider, binder) {
            binder.bind(provider.showNavigationBarSetting).asProfilePreference()
        }
        val showTextSelectionMenu = remember(provider, binder) {
            binder.bind(provider.showTextSelectionMenuSetting).asProfilePreference()
        }
        val showReadingProgress = remember(provider, binder) {
            binder.bind(provider.showReadingProgressSetting).asProfilePreference()
        }
        val readingProgressStyle = remember(provider, binder) {
            binder.bind(provider.readingProgressStyleSetting).asProfilePreference()
        }
        val readingProgressVisible by showReadingProgress.collectAsState()
        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_category_appearance),
                preferenceItems = listOf(
                    Preference.PreferenceItem.ListPreference(
                        preference = theme,
                        entries = mapOf(
                            BookDocumentReaderThemeMode.APP to
                                stringResource(MR.strings.book_document_reader_theme_app),
                            BookDocumentReaderThemeMode.PAPER to
                                stringResource(MR.strings.book_document_reader_theme_paper),
                            BookDocumentReaderThemeMode.DUSK to
                                stringResource(MR.strings.book_document_reader_theme_dusk),
                            BookDocumentReaderThemeMode.BLACK to
                                stringResource(MR.strings.book_document_reader_theme_black),
                        ),
                        title = stringResource(MR.strings.pref_book_document_reader_theme),
                    ),
                    Preference.PreferenceItem.StepperPreference(
                        preference = textSize,
                        valueRange = BookDocumentReaderSettings.TEXT_SIZE_RANGE,
                        step = BookDocumentReaderSettings.TEXT_SIZE_STEP_PERCENT,
                        valueFormatter = { "$it%" },
                        inputSuffix = "%",
                        decreaseContentDescription = stringResource(MR.strings.action_decrease_text_size),
                        increaseContentDescription = stringResource(MR.strings.action_increase_text_size),
                        editContentDescription = stringResource(MR.strings.action_edit_text_size),
                        title = stringResource(MR.strings.pref_book_document_reader_text_size),
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_behavior),
                preferenceItems = listOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = showTextSelectionMenu,
                        title = stringResource(MR.strings.pref_book_document_reader_show_text_selection_menu),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = showStatusBar,
                        title = stringResource(MR.strings.pref_book_document_reader_show_status_bar),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = showNavigationBar,
                        title = stringResource(MR.strings.pref_book_document_reader_show_navigation_bar),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = showReadingProgress,
                        title = stringResource(MR.strings.pref_book_document_reader_show_reading_progress),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        preference = readingProgressStyle,
                        entries = mapOf(
                            BookDocumentReaderProgressStyle.EDGE_FILL_RAIL to
                                stringResource(MR.strings.book_document_reader_progress_edge_fill_rail),
                            BookDocumentReaderProgressStyle.EDGE_POSITION_MARKER to
                                stringResource(MR.strings.book_document_reader_progress_edge_position_marker),
                            BookDocumentReaderProgressStyle.BOTTOM_HAIRLINE to
                                stringResource(MR.strings.book_document_reader_progress_bottom_hairline),
                            BookDocumentReaderProgressStyle.PERCENTAGE to
                                stringResource(MR.strings.book_document_reader_progress_percentage),
                        ),
                        title = stringResource(MR.strings.pref_book_document_reader_reading_progress_style),
                        enabled = readingProgressVisible,
                    ),
                ),
            ),
        )
    }
}
