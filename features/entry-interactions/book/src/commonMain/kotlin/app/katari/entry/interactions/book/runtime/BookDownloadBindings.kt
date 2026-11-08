package app.katari.entry.interactions.book.runtime

import app.katari.core.metro.AppCoroutineScope
import app.katari.core.platform.storage.AppPrivateDirectories
import app.katari.entry.interactions.book.content.BookMaterializationStore
import app.katari.entry.interactions.book.document.preparation.BookDocumentPreparedCache
import app.katari.entry.interactions.book.document.resource.BookDocumentResourceDecoding
import app.katari.entry.interactions.book.document.resource.BookPublicationResourceGatewayFactory
import app.katari.entry.interactions.book.download.BookDownloadCache
import app.katari.entry.interactions.book.download.BookDownloadIndexStore
import app.katari.entry.interactions.book.download.BookDownloadManager
import app.katari.entry.interactions.book.download.BookDownloadProcessor
import app.katari.entry.interactions.book.download.BookDownloadProvider
import app.katari.entry.interactions.book.download.BookDownloadReconciliationStore
import app.katari.entry.interactions.book.download.BookDownloadStore
import app.katari.entry.interactions.book.download.BookDownloader
import app.katari.entry.interactions.book.preparation.BookContentPreparerRegistry
import app.katari.entry.interactions.download.EntryBulkDownloadCandidateProcessor
import app.katari.entry.interactions.download.EntryDownloadProcessor
import app.katari.entry.interactions.download.EntryDownloadWorkController
import app.katari.entry.interactions.runtime.EntryTypeKey
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import tachiyomi.core.common.preference.PlatformPreferenceFile
import tachiyomi.core.common.preference.PlatformPreferenceFiles
import tachiyomi.core.common.storage.StorageFileResolver
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.storage.service.StorageManager

@ContributesTo(AppScope::class)
@BindingContainer
object BookDownloadBindings {
    @Provides
    @SingleIn(AppScope::class)
    internal fun downloadProvider(
        storageManager: StorageManager,
        directories: AppPrivateDirectories,
    ): BookDownloadProvider = BookDownloadProvider(storageManager, BookDownloadReconciliationStore(directories))

    @Provides
    @SingleIn(AppScope::class)
    internal fun downloadCache(
        provider: BookDownloadProvider,
        @AppCoroutineScope scope: CoroutineScope,
        directories: AppPrivateDirectories,
        files: StorageFileResolver,
        storageManager: StorageManager,
    ): BookDownloadCache = BookDownloadCache(
        provider = provider,
        scope = scope,
        indexStore = BookDownloadIndexStore(directories, files),
        storageChanges = storageManager.changes,
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun downloadStore(
        directories: AppPrivateDirectories,
        preferenceFiles: PlatformPreferenceFiles,
        json: Json,
        entryRepository: EntryRepository,
        entryChapterRepository: EntryChapterRepository,
    ): BookDownloadStore = BookDownloadStore(
        directories = directories,
        legacyQueue = preferenceFiles.open(PlatformPreferenceFile.LEGACY_BOOK_DOWNLOAD_QUEUE),
        json = json,
        entryRepository = entryRepository,
        entryChapterRepository = entryChapterRepository,
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun downloader(
        files: StorageFileResolver,
        provider: BookDownloadProvider,
        cache: BookDownloadCache,
        sourceManager: SourceManager,
        networkHelper: NetworkHelper,
        materializationStore: BookMaterializationStore,
        preparerRegistry: BookContentPreparerRegistry,
        decoding: BookDocumentResourceDecoding,
    ): BookDownloader = BookDownloader(
        files = files,
        provider = provider,
        cache = cache,
        sourceManager = sourceManager,
        networkHelper = networkHelper,
        materializationStore = materializationStore,
        preparerRegistry = preparerRegistry,
        decoding = decoding,
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
}
