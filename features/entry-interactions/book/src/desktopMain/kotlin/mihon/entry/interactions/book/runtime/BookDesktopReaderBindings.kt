package mihon.entry.interactions.book.runtime

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.book.content.BookMaterializationCache
import mihon.entry.interactions.book.media.BookMediaCacheProvider
import mihon.entry.interactions.book.navigation.BookOpenProcessor
import mihon.entry.interactions.book.reader.window.DesktopBookReader
import mihon.entry.interactions.media.EntryMediaCacheProvider
import mihon.entry.interactions.navigation.EntryOpenProcessor
import mihon.entry.interactions.reader.window.BookReader
import mihon.entry.interactions.reader.window.BookReaderDestination
import mihon.entry.interactions.runtime.EntryTypeKey

/**
 * The desktop's book reader: chapters open in the screen the app's [BookReaderDestination] shows, read in the shared
 * reader, and the materialized resources it opens books from are a cache the settings can clear.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object BookDesktopReaderBindings {
    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.BOOK)
    internal fun open(destination: BookReaderDestination): EntryOpenProcessor = BookOpenProcessor(destination)

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.BOOK)
    internal fun mediaCache(
        materializationCache: () -> BookMaterializationCache,
    ): EntryMediaCacheProvider = BookMediaCacheProvider(materializationCache)

    @Provides
    internal fun reader(reader: DesktopBookReader): BookReader = reader
}
