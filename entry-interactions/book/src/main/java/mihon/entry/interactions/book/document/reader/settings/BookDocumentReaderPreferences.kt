package mihon.entry.interactions.book.document.reader.settings

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum

internal class BookDocumentReaderPreferences(
    preferenceStore: PreferenceStore,
) {
    val themeMode: Preference<BookDocumentReaderThemeMode> =
        preferenceStore.getEnum(THEME_MODE_KEY, BookDocumentReaderThemeMode.APP)
    val showStatusBar: Preference<Boolean> = preferenceStore.getBoolean(SHOW_STATUS_BAR_KEY, false)

    companion object {
        const val KEY_PREFIX = "book_document_reader_"
        const val THEME_MODE_KEY = "book_document_reader_theme_mode"
        const val SHOW_STATUS_BAR_KEY = "book_document_reader_show_status_bar"
    }
}
