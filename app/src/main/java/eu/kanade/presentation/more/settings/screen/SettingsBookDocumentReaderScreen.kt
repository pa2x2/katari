package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import eu.kanade.presentation.more.settings.Preference
import mihon.entry.interactions.reader.settings.BookDocumentReaderSettings
import mihon.entry.interactions.reader.settings.BookDocumentReaderThemeMode
import mihon.entry.viewer.settings.ViewerSettingBinder
import mihon.entry.viewer.settings.asProfilePreference
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
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
        return listOf(
            Preference.PreferenceItem.ListPreference(
                preference = theme,
                entries = mapOf(
                    BookDocumentReaderThemeMode.APP to stringResource(MR.strings.book_document_reader_theme_app),
                    BookDocumentReaderThemeMode.BLACK to stringResource(MR.strings.book_document_reader_theme_black),
                ),
                title = stringResource(MR.strings.pref_book_document_reader_theme),
            ),
        )
    }
}
