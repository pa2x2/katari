package mihon.entry.interactions.book.runtime

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.book.content.BookMaterializationCache
import mihon.entry.interactions.book.download.BookDownloadCache
import mihon.entry.interactions.book.download.BookDownloadManager
import mihon.entry.interactions.book.download.BookDownloadProcessor
import mihon.entry.interactions.book.media.BookMediaCacheProvider
import mihon.entry.interactions.book.media.session.BookMediaSessionProcessor
import mihon.entry.interactions.book.navigation.BookOpenProcessor
import mihon.entry.interactions.download.EntryBulkDownloadCandidateProcessor
import mihon.entry.interactions.download.EntryDownloadProcessor
import mihon.entry.interactions.media.EntryMediaCacheProvider
import mihon.entry.interactions.media.EntryMediaSessionProcessor
import mihon.entry.interactions.navigation.EntryOpenProcessor
import mihon.entry.interactions.runtime.EntryTypeKey
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.source.service.SourceManager

/**
 * Book's contributions that need the Android reader or downloads: opening, downloads, the reader's media session
 * events and the materialized resource cache. The rest is in [BookEntryTypeBindings], which every platform has.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object BookEntryInteractionProviderBindings {
    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.BOOK)
    internal fun open(): EntryOpenProcessor = BookOpenProcessor()

    @Provides
    @SingleIn(AppScope::class)
    internal fun downloadProcessor(
        downloadManager: BookDownloadManager,
        downloadCache: BookDownloadCache,
        sourceManager: SourceManager,
        entryRepository: EntryRepository,
        getEntryWithChapters: GetEntryWithChapters,
    ): BookDownloadProcessor = BookDownloadProcessor(
        manager = downloadManager,
        cache = downloadCache,
        sourceManager = sourceManager,
        entryRepository = entryRepository,
        getEntryWithChapters = getEntryWithChapters,
    )

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.BOOK)
    internal fun download(processor: BookDownloadProcessor): EntryDownloadProcessor = processor

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.BOOK)
    internal fun bulkDownloadCandidates(processor: BookDownloadProcessor): EntryBulkDownloadCandidateProcessor =
        processor

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.BOOK)
    internal fun mediaSession(processor: BookMediaSessionProcessor): EntryMediaSessionProcessor = processor

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.BOOK)
    internal fun mediaCache(materializationCache: () -> BookMaterializationCache): EntryMediaCacheProvider =
        BookMediaCacheProvider(materializationCache)
}
