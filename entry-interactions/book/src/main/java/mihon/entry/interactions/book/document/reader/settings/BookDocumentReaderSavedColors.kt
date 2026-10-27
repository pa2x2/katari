package mihon.entry.interactions.book.document.reader.settings

import mihon.entry.interactions.reader.settings.BookDocumentReaderTheme
import mihon.entry.viewer.settings.ViewerSettingBinding
import tachiyomi.core.common.preference.Preference

/**
 * The profile's saved reader colours and the book's choice among the themes. Saving or editing colours shows them in
 * this book; where the profile used colours that are edited or deleted, its choice follows.
 */
internal class BookDocumentReaderSavedColors(
    private val saved: Preference<List<BookDocumentReaderTheme.Custom>>,
    private val theme: ViewerSettingBinding<BookDocumentReaderTheme>,
) {
    /** Picking the profile's own theme clears this book's override instead of pinning the book to it. */
    suspend fun choose(choice: BookDocumentReaderTheme) {
        val setting = theme.state.value
        choose(choice, profileTheme = setting.profileValue ?: setting.processorDefault)
    }

    private suspend fun choose(choice: BookDocumentReaderTheme, profileTheme: BookDocumentReaderTheme) {
        if (choice == profileTheme) {
            theme.clearEntryOverride()
        } else {
            theme.setEntryOverride(choice)
        }
    }

    /** Saves [colors], in place of [replacing] when editing saved colours. */
    suspend fun save(colors: BookDocumentReaderTheme.Custom, replacing: BookDocumentReaderTheme.Custom? = null) {
        val current = saved.get()
        saved.set(
            when {
                replacing != null && replacing in current -> current.map { if (it == replacing) colors else it }
                else -> current + colors
            }.distinct(),
        )
        val setting = theme.state.value
        val profileTheme = if (replacing != null && setting.profileValue == replacing) {
            theme.setProfileValue(colors)
            colors
        } else {
            setting.profileValue ?: setting.processorDefault
        }
        choose(colors, profileTheme)
    }

    suspend fun delete(colors: BookDocumentReaderTheme.Custom) {
        saved.set(saved.get() - colors)
        val setting = theme.state.value
        if (setting.entryOverride == colors) theme.clearEntryOverride()
        if (setting.profileValue == colors) theme.resetProfileValue()
    }
}
