package mihon.entry.interactions.book.runtime

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.book.child.BookChildListProcessor
import mihon.entry.interactions.book.content.BookMaterializationCache
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderSettingsProvider
import mihon.entry.interactions.book.download.BookDownloadCache
import mihon.entry.interactions.book.download.BookDownloadManager
import mihon.entry.interactions.book.download.BookDownloadProcessor
import mihon.entry.interactions.book.library.BookLibraryProgressProvider
import mihon.entry.interactions.book.library.BookOutsideReleasePeriodFilterProvider
import mihon.entry.interactions.book.media.BookMediaCacheProvider
import mihon.entry.interactions.book.media.session.BookMediaSessionProcessor
import mihon.entry.interactions.book.migration.BookMigrationProvider
import mihon.entry.interactions.book.navigation.BookContinueProcessor
import mihon.entry.interactions.book.navigation.BookOpenProcessor
import mihon.entry.interactions.book.presentation.BookEntryTypePresentationProvider
import mihon.entry.interactions.book.state.BookConsumptionProcessor
import mihon.entry.interactions.book.state.BookProgressProcessor
import mihon.entry.interactions.book.state.BookTranslationLanguagesProvider
import mihon.entry.interactions.book.statistics.BookEntryStatisticsProvider
import mihon.entry.interactions.download.EntryBulkDownloadCandidateProcessor
import mihon.entry.interactions.download.EntryDownloadProcessor
import mihon.entry.interactions.library.EntryLibraryProgressProvider
import mihon.entry.interactions.media.DefaultEntryViewerSettingsProvider
import mihon.entry.interactions.media.EntryMediaCacheProvider
import mihon.entry.interactions.media.EntryMediaSessionProcessor
import mihon.entry.interactions.media.EntryViewerSettingsProvider
import mihon.entry.interactions.navigation.EntryContinueProcessor
import mihon.entry.interactions.navigation.EntryOpenProcessor
import mihon.entry.interactions.runtime.EntryChildListProcessor
import mihon.entry.interactions.runtime.EntryChildProgressProcessor
import mihon.entry.interactions.runtime.EntryOutsideReleasePeriodFilterProvider
import mihon.entry.interactions.runtime.EntryStatisticsProvider
import mihon.entry.interactions.runtime.EntryTypeKey
import mihon.entry.interactions.runtime.EntryTypePresentationProvider
import mihon.entry.interactions.state.EntryConsumptionProcessor
import mihon.entry.interactions.state.EntryMigrationProvider
import mihon.entry.interactions.state.EntryProgressProcessor
import mihon.entry.interactions.state.EntryTranslationLanguagesProvider
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.source.service.SourceManager

@ContributesTo(AppScope::class)
@BindingContainer
object BookEntryInteractionProviderBindings {
    @Provides
    @SingleIn(AppScope::class)
    internal fun openProcessor(): BookOpenProcessor = BookOpenProcessor()

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.BOOK)
    internal fun open(processor: BookOpenProcessor): EntryOpenProcessor = processor

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.BOOK)
    @SingleIn(AppScope::class)
    internal fun continueEntry(
        getEntryWithChapters: GetEntryWithChapters,
        entryProgressRepository: EntryProgressRepository,
        openProcessor: BookOpenProcessor,
    ): EntryContinueProcessor = BookContinueProcessor(getEntryWithChapters, entryProgressRepository, openProcessor)

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.BOOK)
    @SingleIn(AppScope::class)
    internal fun consumption(
        entryProgressRepository: EntryProgressRepository,
        entryChapterRepository: EntryChapterRepository,
    ): EntryConsumptionProcessor = BookConsumptionProcessor(entryProgressRepository, entryChapterRepository)

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.BOOK)
    @SingleIn(AppScope::class)
    internal fun progress(
        entryProgressRepository: EntryProgressRepository,
        entryChapterRepository: EntryChapterRepository,
    ): EntryProgressProcessor = BookProgressProcessor(entryProgressRepository, entryChapterRepository)

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
    internal fun migration(): EntryMigrationProvider = BookMigrationProvider()

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.BOOK)
    internal fun translationLanguages(): EntryTranslationLanguagesProvider = BookTranslationLanguagesProvider

    @Provides
    @SingleIn(AppScope::class)
    internal fun childListProcessor(entryProgressRepository: EntryProgressRepository): BookChildListProcessor =
        BookChildListProcessor(entryProgressRepository)

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.BOOK)
    internal fun childList(processor: BookChildListProcessor): EntryChildListProcessor = processor

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.BOOK)
    internal fun childProgress(processor: BookChildListProcessor): EntryChildProgressProcessor = processor

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.BOOK)
    @SingleIn(AppScope::class)
    internal fun libraryProgress(entryProgressRepository: EntryProgressRepository): EntryLibraryProgressProvider =
        BookLibraryProgressProvider(entryProgressRepository)

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.BOOK)
    internal fun outsideReleasePeriodFilter(): EntryOutsideReleasePeriodFilterProvider =
        BookOutsideReleasePeriodFilterProvider()

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.BOOK)
    internal fun mediaSession(processor: BookMediaSessionProcessor): EntryMediaSessionProcessor = processor

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.BOOK)
    internal fun typePresentation(): EntryTypePresentationProvider = BookEntryTypePresentationProvider

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.BOOK)
    internal fun statistics(): EntryStatisticsProvider = BookEntryStatisticsProvider

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.BOOK)
    internal fun mediaCache(materializationCache: () -> BookMaterializationCache): EntryMediaCacheProvider =
        BookMediaCacheProvider(materializationCache)

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.BOOK)
    @SingleIn(AppScope::class)
    internal fun viewerSettings(provider: BookDocumentReaderSettingsProvider): EntryViewerSettingsProvider =
        DefaultEntryViewerSettingsProvider(surfaces = listOf(provider))
}
