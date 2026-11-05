package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import mihon.desktop.reader.WindowBookReader
import mihon.desktop.reader.WindowMangaReader
import mihon.entry.interactions.reader.window.BookReaderDestination
import mihon.entry.interactions.reader.window.MangaReaderDestination

@BindingContainer
object ReaderBindings {
    @Provides
    fun mangaReaderDestination(reader: WindowMangaReader): MangaReaderDestination = reader

    @Provides
    fun bookReaderDestination(reader: WindowBookReader): BookReaderDestination = reader
}
