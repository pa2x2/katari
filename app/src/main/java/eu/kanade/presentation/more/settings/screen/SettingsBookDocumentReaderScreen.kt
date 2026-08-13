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
        val showStatusBar = remember(provider, binder) {
            binder.bind(provider.showStatusBarSetting).asProfilePreference()
        }
        val showReadingProgress = remember(provider, binder) {
            binder.bind(provider.showReadingProgressSetting).asProfilePreference()
        }
        val readingProgressStyle = remember(provider, binder) {
            binder.bind(provider.readingProgressStyleSetting).asProfilePreference()
        }
        val readingProgressVisible by showReadingProgress.collectAsState()
        return listOf(
            Preference.PreferenceItem.ListPreference(
                preference = theme,
                entries = mapOf(
                    BookDocumentReaderThemeMode.APP to stringResource(MR.strings.book_document_reader_theme_app),
                    BookDocumentReaderThemeMode.BLACK to stringResource(MR.strings.book_document_reader_theme_black),
                ),
                title = stringResource(MR.strings.pref_book_document_reader_theme),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = showStatusBar,
                title = stringResource(MR.strings.pref_book_document_reader_show_status_bar),
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
        )
    }
}
