package mihon.entry.interactions.book.document.reader.settings

import mihon.entry.interactions.reader.settings.BookDocumentReaderSettings
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum

internal class BookDocumentReaderPreferences(
    preferenceStore: PreferenceStore,
) {
    val themeMode: Preference<BookDocumentReaderThemeMode> =
        preferenceStore.getEnum(THEME_MODE_KEY, BookDocumentReaderThemeMode.APP)
    val textSizePercent: Preference<Int> = preferenceStore.getInt(
        TEXT_SIZE_PERCENT_KEY,
        BookDocumentReaderSettings.DEFAULT_TEXT_SIZE_PERCENT,
    )
    val keepScreenAlive: Preference<Boolean> = preferenceStore.getBoolean(
        KEEP_SCREEN_ALIVE_KEY,
        BookDocumentReaderSettings.DEFAULT_KEEP_SCREEN_ALIVE,
    )
    val showStatusBar: Preference<Boolean> = preferenceStore.getBoolean(SHOW_STATUS_BAR_KEY, false)
    val showNavigationBar: Preference<Boolean> = preferenceStore.getBoolean(SHOW_NAVIGATION_BAR_KEY, false)
    val showTextSelectionMenu: Preference<Boolean> = preferenceStore.getBoolean(SHOW_TEXT_SELECTION_MENU_KEY, true)
    val showReadingProgress: Preference<Boolean> = preferenceStore.getBoolean(SHOW_READING_PROGRESS_KEY, true)
    val readingProgressStyle: Preference<BookDocumentReaderProgressStyle> =
        preferenceStore.getEnum(READING_PROGRESS_STYLE_KEY, BookDocumentReaderProgressStyle.PERCENTAGE)

    companion object {
        const val KEY_PREFIX = "book_document_reader_"
        const val THEME_MODE_KEY = "book_document_reader_theme_mode"
        const val TEXT_SIZE_PERCENT_KEY = "book_document_reader_text_size_percent"
        const val KEEP_SCREEN_ALIVE_KEY = "book_document_reader_keep_screen_alive"
        const val SHOW_STATUS_BAR_KEY = "book_document_reader_show_status_bar"
        const val SHOW_NAVIGATION_BAR_KEY = "book_document_reader_show_navigation_bar"
        const val SHOW_TEXT_SELECTION_MENU_KEY = "book_document_reader_show_text_selection_menu"
        const val SHOW_READING_PROGRESS_KEY = "book_document_reader_show_reading_progress"
        const val READING_PROGRESS_STYLE_KEY = "book_document_reader_reading_progress_style"
    }
}
