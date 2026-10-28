package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.textrules.BookTextRulesDestination
import mihon.desktop.book.NoBookDocumentReaderFontLibrary
import mihon.desktop.book.NoBookTextRulesDestination
import mihon.entry.interactions.reader.settings.BookDocumentReaderFontLibrary

@BindingContainer
interface BookReaderBindings {
    @Binds
    val NoBookDocumentReaderFontLibrary.bookDocumentReaderFontLibrary: BookDocumentReaderFontLibrary

    @Binds
    val NoBookTextRulesDestination.bookTextRulesDestination: BookTextRulesDestination
}
