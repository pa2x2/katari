package app.katari.entry.interactions.book.runtime

import app.katari.entry.interactions.book.content.BookMaterializationCache
import app.katari.entry.interactions.book.media.BookMediaCacheProvider
import app.katari.entry.interactions.book.navigation.BookOpenProcessor
import app.katari.entry.interactions.media.EntryMediaCacheProvider
import app.katari.entry.interactions.navigation.EntryOpenProcessor
import app.katari.entry.interactions.runtime.EntryTypeKey
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import eu.kanade.tachiyomi.source.entry.EntryType

/**
 * Book's contributions that need the Android reader: opening and the materialized resource cache. The rest is in
 * [BookEntryTypeBindings], [BookEntryInteractionBindings] and [BookDownloadBindings], which every platform has.
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
    internal fun mediaCache(
        materializationCache: () -> BookMaterializationCache,
    ): EntryMediaCacheProvider = BookMediaCacheProvider(materializationCache)
}
