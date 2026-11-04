package mihon.entry.interactions.book.runtime

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.NetworkHelper
import mihon.entry.interactions.book.content.BookMaterializationStore
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderPreferences
import mihon.entry.interactions.book.document.resource.AndroidBookDocumentResourceDecoding
import mihon.entry.interactions.book.document.resource.BookDocumentResourceDecoding
import mihon.entry.interactions.book.download.BookDownloadCache
import mihon.entry.interactions.book.media.session.BookMediaSessionProcessor
import mihon.entry.interactions.book.navigation.BookChapterNavigationResolver
import mihon.entry.interactions.book.preparation.BookContentPreparerRegistry
import mihon.entry.interactions.book.processor.BookReaderProcessorPreferences
import mihon.entry.interactions.book.processor.BookReaderProcessorRegistry
import mihon.entry.interactions.book.processor.BookReaderProcessorSelectionCoordinator
import mihon.entry.interactions.book.reader.BookReaderHostResolver
import mihon.entry.interactions.book.reader.BookReaderSessionFactory
import mihon.entry.interactions.book.reader.BookReaderSessionRegistry
import mihon.entry.interactions.media.session.EntryMediaSessionEventSink
import tachiyomi.core.common.storage.StorageFileResolver
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.source.service.SourceManager

/**
 * The Android book reader's services: reader sessions and the checks that its images and fonts draw. The settings,
 * preferences and reader registry they read are in [BookEntryTypeBindings], and publication preparation and downloads
 * in [BookPreparationBindings] and [BookDownloadBindings].
 */
@ContributesTo(AppScope::class)
@BindingContainer
object BookEntryInteractionBindings {
    @Provides
    internal fun resourceDecoding(): BookDocumentResourceDecoding = AndroidBookDocumentResourceDecoding

    @Provides
    @SingleIn(AppScope::class)
    internal fun mediaSessionProcessor(sink: EntryMediaSessionEventSink): BookMediaSessionProcessor =
        BookMediaSessionProcessor(sink)

    @Provides
    @SingleIn(AppScope::class)
    internal fun readerSessionRegistry(): BookReaderSessionRegistry = BookReaderSessionRegistry()

    @Provides
    @SingleIn(AppScope::class)
    internal fun chapterNavigationResolver(getEntryWithChapters: GetEntryWithChapters): BookChapterNavigationResolver =
        BookChapterNavigationResolver(getEntryWithChapters)

    @Provides
    @SingleIn(AppScope::class)
    internal fun processorSelectionCoordinator(
        registry: BookReaderProcessorRegistry,
        preferences: BookReaderProcessorPreferences,
    ): BookReaderProcessorSelectionCoordinator = BookReaderProcessorSelectionCoordinator(
        registry = registry,
        preferences = preferences,
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun readerHostResolver(
        sessionFactory: BookReaderSessionFactory,
        preparerRegistry: BookContentPreparerRegistry,
        selectionCoordinator: BookReaderProcessorSelectionCoordinator,
    ): BookReaderHostResolver = BookReaderHostResolver(
        sessionFactory = sessionFactory,
        preparerRegistry = preparerRegistry,
        selectionCoordinator = selectionCoordinator,
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun readerSessionFactory(
        entryRepository: EntryRepository,
        entryChapterRepository: EntryChapterRepository,
        entryProgressRepository: EntryProgressRepository,
        sourceManager: SourceManager,
        preparerRegistry: BookContentPreparerRegistry,
        readerProcessorRegistry: BookReaderProcessorRegistry,
        networkHelper: NetworkHelper,
        files: StorageFileResolver,
        materializationStore: BookMaterializationStore,
        downloadCache: BookDownloadCache,
        mediaSession: BookMediaSessionProcessor,
        documentReaderPreferences: BookDocumentReaderPreferences,
    ): BookReaderSessionFactory = BookReaderSessionFactory(
        entryRepository = entryRepository,
        entryChapterRepository = entryChapterRepository,
        entryProgressRepository = entryProgressRepository,
        sourceManager = sourceManager,
        preparerRegistry = preparerRegistry,
        readerProcessorRegistry = readerProcessorRegistry,
        networkHelper = networkHelper,
        files = files,
        materializationStore = materializationStore,
        downloadCache = downloadCache,
        mediaSession = mediaSession,
        textRules = { documentReaderPreferences.textRules.get() },
    )
}
