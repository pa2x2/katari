package mihon.entry.interactions.book.document.reader.settings

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum

internal class BookDocumentReaderPreferences(
    preferenceStore: PreferenceStore,
) {
    val themeMode: Preference<BookDocumentReaderThemeMode> =
        preferenceStore.getEnum(THEME_MODE_KEY, BookDocumentReaderThemeMode.APP)

    companion object {
        const val KEY_PREFIX = "book_document_reader_"
        const val THEME_MODE_KEY = "book_document_reader_theme_mode"
    }
}
