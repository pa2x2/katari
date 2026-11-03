package mihon.entry.interactions.book.runtime

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import mihon.core.metro.AppCoroutineScope
import mihon.entry.interactions.book.content.BookMaterializationCache
import mihon.entry.interactions.book.content.BookMaterializationStore
import mihon.entry.interactions.book.document.preparation.BookDocumentPreparedCache
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderPreferences
import mihon.entry.interactions.book.document.resource.BookPublicationResourceGatewayFactory
import mihon.entry.interactions.book.download.BookDownloadCache
import mihon.entry.interactions.book.download.BookDownloadIndexStore
import mihon.entry.interactions.book.download.BookDownloadManager
import mihon.entry.interactions.book.download.BookDownloadProvider
import mihon.entry.interactions.book.download.BookDownloadStore
import mihon.entry.interactions.book.download.BookDownloader
import mihon.entry.interactions.book.format.epub.preparation.EpubBookPreparer
import mihon.entry.interactions.book.format.html.prosechapter.preparation.HtmlProseChapterPreparer
import mihon.entry.interactions.book.media.session.BookMediaSessionProcessor
import mihon.entry.interactions.book.navigation.BookChapterNavigationResolver
import mihon.entry.interactions.book.preparation.BookContentPreparerRegistry
import mihon.entry.interactions.book.processor.BookReaderProcessorPreferences
import mihon.entry.interactions.book.processor.BookReaderProcessorRegistry
import mihon.entry.interactions.book.processor.BookReaderProcessorSelectionCoordinator
import mihon.entry.interactions.book.reader.BookReaderHostResolver
import mihon.entry.interactions.book.reader.BookReaderSessionFactory
import mihon.entry.interactions.book.reader.BookReaderSessionRegistry
import mihon.entry.interactions.download.EntryDownloadWorkController
import mihon.entry.interactions.media.session.EntryMediaSessionEventSink
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.storage.service.StorageManager

/**
 * The Android book reader's services: publication preparation and its caches, downloads and reader sessions. The
 * settings, preferences and reader registry they read are in [BookEntryTypeBindings].
 */
@ContributesTo(AppScope::class)
@BindingContainer
object BookEntryInteractionBindings {
    @Provides
    @SingleIn(AppScope::class)
    internal fun mediaSessionProcessor(sink: EntryMediaSessionEventSink): BookMediaSessionProcessor =
        BookMediaSessionProcessor(sink)

    @Provides
    @SingleIn(AppScope::class)
    internal fun materializationCache(application: Application): BookMaterializationCache =
        BookMaterializationCache(application)

    @Provides
    internal fun materializationStore(cache: BookMaterializationCache): BookMaterializationStore = cache

    @Provides
    @SingleIn(AppScope::class)
    internal fun preparedDocumentCache(application: Application): BookDocumentPreparedCache =
        BookDocumentPreparedCache(application)

    @Provides
    @SingleIn(AppScope::class)
    internal fun resourceGatewayFactory(
        application: Application,
        networkHelper: NetworkHelper,
    ): BookPublicationResourceGatewayFactory = BookPublicationResourceGatewayFactory(application, networkHelper.client)

    @Provides
    @SingleIn(AppScope::class)
    internal fun preparerRegistry(
        preparedDocumentCache: BookDocumentPreparedCache,
        resourceGatewayFactory: BookPublicationResourceGatewayFactory,
    ): BookContentPreparerRegistry = BookContentPreparerRegistry(
        preparers = listOf(
            HtmlProseChapterPreparer(),
            EpubBookPreparer(preparedDocumentCache, resourceGatewayFactory),
        ),
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun downloadProvider(storageManager: StorageManager, application: Application): BookDownloadProvider =
        BookDownloadProvider(storageManager, application)

    @Provides
    @SingleIn(AppScope::class)
    internal fun downloadCache(
        provider: BookDownloadProvider,
        @AppCoroutineScope scope: CoroutineScope,
        application: Application,
        storageManager: StorageManager,
    ): BookDownloadCache = BookDownloadCache(
        provider = provider,
        scope = scope,
        indexStore = BookDownloadIndexStore(application),
        storageChanges = storageManager.changes,
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun downloadStore(
        application: Application,
        json: Json,
        entryRepository: EntryRepository,
        entryChapterRepository: EntryChapterRepository,
    ): BookDownloadStore = BookDownloadStore(application, json, entryRepository, entryChapterRepository)

    @Provides
    @SingleIn(AppScope::class)
    internal fun downloader(
        application: Application,
        provider: BookDownloadProvider,
        cache: BookDownloadCache,
        sourceManager: SourceManager,
        networkHelper: NetworkHelper,
        materializationStore: BookMaterializationStore,
        preparerRegistry: BookContentPreparerRegistry,
    ): BookDownloader = BookDownloader(
        application = application,
        provider = provider,
        cache = cache,
        sourceManager = sourceManager,
        networkHelper = networkHelper,
        materializationStore = materializationStore,
        preparerRegistry = preparerRegistry,
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun downloadManager(
        cache: BookDownloadCache,
        provider: BookDownloadProvider,
        downloader: BookDownloader,
        preparedDocumentCache: BookDocumentPreparedCache,
        resourceGatewayFactory: BookPublicationResourceGatewayFactory,
        sourceManager: SourceManager,
        store: BookDownloadStore,
        workController: EntryDownloadWorkController,
        @AppCoroutineScope scope: CoroutineScope,
    ): BookDownloadManager = BookDownloadManager(
        cache = cache,
        provider = provider,
        downloader = downloader,
        preparedDocumentCache = preparedDocumentCache,
        resourceGatewayFactory = resourceGatewayFactory,
        sourceManager = sourceManager,
        store = store,
        workController = workController,
        scope = scope,
    )

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
        materializationStore = materializationStore,
        downloadCache = downloadCache,
        mediaSession = mediaSession,
        textRules = { documentReaderPreferences.textRules.get() },
    )
}
