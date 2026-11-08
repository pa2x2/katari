package app.katari.desktop.di

import app.katari.desktop.reader.WindowBookReader
import app.katari.desktop.reader.WindowMangaReader
import app.katari.entry.interactions.reader.window.BookReaderDestination
import app.katari.entry.interactions.reader.window.MangaReaderDestination
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides

@BindingContainer
object ReaderBindings {
    @Provides
    fun mangaReaderDestination(reader: WindowMangaReader): MangaReaderDestination = reader

    @Provides
    fun bookReaderDestination(reader: WindowBookReader): BookReaderDestination = reader
}
