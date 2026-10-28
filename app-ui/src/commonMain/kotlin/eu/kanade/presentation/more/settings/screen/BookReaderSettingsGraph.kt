package eu.kanade.presentation.more.settings.screen

import eu.kanade.presentation.more.settings.screen.textrules.BookTextRulesDestination
import mihon.entry.interactions.reader.settings.BookDocumentReaderFontLibrary
import tachiyomi.data.ActiveProfileProvider

/** Graph entry points of the book reader settings, beyond the settings its surface provides. */
interface BookReaderSettingsGraph {
    /** Lists the active profile's imported fonts, which the font setting offers. */
    val bookDocumentReaderFontLibrary: BookDocumentReaderFontLibrary
    val activeProfileProvider: ActiveProfileProvider
    val bookTextRulesDestination: BookTextRulesDestination
}
