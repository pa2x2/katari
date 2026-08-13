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
    val showReadingProgress: Preference<Boolean> = preferenceStore.getBoolean(SHOW_READING_PROGRESS_KEY, true)
    val readingProgressStyle: Preference<BookDocumentReaderProgressStyle> =
        preferenceStore.getEnum(READING_PROGRESS_STYLE_KEY, BookDocumentReaderProgressStyle.PERCENTAGE)

    companion object {
        const val KEY_PREFIX = "book_document_reader_"
        const val THEME_MODE_KEY = "book_document_reader_theme_mode"
        const val SHOW_STATUS_BAR_KEY = "book_document_reader_show_status_bar"
        const val SHOW_READING_PROGRESS_KEY = "book_document_reader_show_reading_progress"
        const val READING_PROGRESS_STYLE_KEY = "book_document_reader_reading_progress_style"
    }
}
