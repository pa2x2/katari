package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import mihon.desktop.book.NoBookDocumentReaderFontLibrary
import mihon.entry.interactions.reader.settings.BookDocumentReaderFontLibrary

@BindingContainer
interface BookReaderBindings {
    @Binds
    val NoBookDocumentReaderFontLibrary.bookDocumentReaderFontLibrary: BookDocumentReaderFontLibrary
}
