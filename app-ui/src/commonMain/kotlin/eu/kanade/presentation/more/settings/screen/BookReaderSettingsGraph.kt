package eu.kanade.presentation.more.settings.screen

import mihon.entry.interactions.reader.settings.BookDocumentReaderFontLibrary
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.source.repository.SourceRepository

/** Graph entry points of the book reader settings, beyond the settings its surface provides. */
interface BookReaderSettingsGraph {
    /** Lists the active profile's imported fonts, which the font setting offers. */
    val bookDocumentReaderFontLibrary: BookDocumentReaderFontLibrary
    val activeProfileProvider: ActiveProfileProvider

    /** Lists the book sources that text rules can be added for. */
    val sourceRepository: SourceRepository
}
