package mihon.entry.interactions.book.runtime

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.book.content.BookMaterializationCache
import mihon.entry.interactions.book.media.BookMediaCacheProvider
import mihon.entry.interactions.book.media.session.BookMediaSessionProcessor
import mihon.entry.interactions.book.navigation.BookOpenProcessor
import mihon.entry.interactions.media.EntryMediaCacheProvider
import mihon.entry.interactions.media.EntryMediaSessionProcessor
import mihon.entry.interactions.navigation.EntryOpenProcessor
import mihon.entry.interactions.runtime.EntryTypeKey

/**
 * Book's contributions that need the Android reader: opening, the reader's media session events and the materialized
 * resource cache. The rest is in [BookEntryTypeBindings] and [BookDownloadBindings], which every platform has.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object BookEntryInteractionProviderBindings {
    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.BOOK)
    internal fun open(): EntryOpenProcessor = BookOpenProcessor()

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.BOOK)
    internal fun mediaSession(processor: BookMediaSessionProcessor): EntryMediaSessionProcessor = processor

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.BOOK)
    internal fun mediaCache(
        application: Application,
        materializationCache: () -> BookMaterializationCache,
    ): EntryMediaCacheProvider = BookMediaCacheProvider(application, materializationCache)
}
