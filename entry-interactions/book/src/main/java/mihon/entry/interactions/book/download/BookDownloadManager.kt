package mihon.entry.interactions.book.download

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import mihon.entry.interactions.book.document.preparation.BookDocumentPreparedCache
import mihon.entry.interactions.book.document.resource.BookPublicationResourceGatewayFactory
import mihon.entry.interactions.book.download.model.BookDownload
import mihon.entry.interactions.book.download.model.BookDownloadFailure
import mihon.entry.interactions.download.EntryDownloadEntryIdentity
import mihon.entry.interactions.download.EntryDownloadEvent
import mihon.entry.interactions.download.EntryDownloadMessage
import mihon.entry.interactions.download.EntryDownloadQueuePolicy
import mihon.entry.interactions.download.EntryDownloadWorkController
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class BookDownloadManager(
    context: Context,
    private val cache: BookDownloadCache = Injekt.get(),
    private val provider: BookDownloadProvider = Injekt.get(),
    private val downloader: BookDownloader = Injekt.get(),
    private val preparedDocumentCache: BookDocumentPreparedCache = Injekt.get(),
    private val resourceGatewayFactory: BookPublicationResourceGatewayFactory = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val store: BookDownloadStore = BookDownloadStore(context),
    private val workController: EntryDownloadWorkController = Injekt.get(),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val queueMutationLock = Any()
    private val processorMutex = Mutex()
    private val initialized = CompletableDeferred<Unit>()
    private val _queueState = MutableStateFlow<List<BookDownload>>(emptyList())
    val queueState = _queueState.asStateFlow()
    val cacheChanges = cache.changes
    val cachePackageUpdates: Flow<BookDownloadPackageUpdate>
        get() = cache.packageUpdates
    private val _isRunning = MutableStateFlow(false)
    val isRunning = _isRunning.asStateFlow()
    private val _events = MutableSharedFlow<EntryDownloadEvent>(replay = 16, extraBufferCapacity = 16)
    val events = _events.asSharedFlow()
    private val statusUpdates = MutableSharedFlow<BookDownload>(
        extraBufferCapacity = UPDATE_BUFFER_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val progressUpdates = MutableSharedFlow<BookDownload>(
        extraBufferCapacity = UPDATE_BUFFER_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    @Volatile
    private var activeChapterId: Long? = null

    @Volatile
    private var activeDownloadJob: Job? = null

    init {
        scope.launch {
            try {
                cache.ensureInitialized()
                val restored = store.restore().filterNot { download ->
                    cache.isDownloaded(
                        BookDownloadPackageKey(
                            sourceId = download.entry.source,
                            entryUrl = download.entry.url,
                            childUrl = download.chapter.url,
                        ),
                    )
                }
                mergeRestoredQueue(restored)
                if (queueState.value.isNotEmpty()) workController.resumeIfRequested()
            } finally {
                initialized.complete(Unit)
            }
        }
    }

    fun startDownloads() {
        if (queueState.value.isEmpty()) return
        queueState.value.forEach { download ->
            if (!download.status.isActive) {
                download.failure = null
                download.status = BookDownload.State.QUEUE
                statusUpdates.tryEmit(download)
            }
        }
        workController.start()
    }

    fun pauseDownloads() {
        val hasQueuedDownloads = synchronized(queueMutationLock) {
            if (_queueState.value.isEmpty()) {
                false
            } else {
                _queueState.value
                    .filter {
                        it.status.isActive
                    }
                    .forEach { it.status = BookDownload.State.QUEUE }
                _isRunning.value = false
                true
            }
        }
        workController.stop()
        synchronized(queueMutationLock) {
            if (!hasQueuedDownloads || _queueState.value.isEmpty()) {
                _isRunning.value = false
            }
        }
    }

    fun clearQueue() {
        workController.stop()
        synchronized(queueMutationLock) {
            _queueState.value = emptyList()
            store.clear()
        }
        _isRunning.value = false
    }

    suspend fun queueBooks(entry: Entry, chapters: List<EntryChapter>, autoStart: Boolean = true) {
        if (chapters.isEmpty()) return
        cache.ensureInitialized()
        val queued = chapters
            .filterNot { cache.isDownloaded(BookDownloadPackageKey(entry.source, entry.url, it.url)) }
            .sortedByDescending(EntryChapter::sourceOrder)
            .toQueuedBookDownloads(entry)
        synchronized(queueMutationLock) {
            val chapterIds = chapters.mapTo(mutableSetOf(), EntryChapter::id)
            _queueState.update { current -> current.filterNot { it.chapter.id in chapterIds } + queued }
            rewriteStoredQueueLocked()
        }
        queued.forEach(statusUpdates::tryEmit)
        if (autoStart) startDownloads()
    }

    fun startDownloadsNow(chapterIds: Collection<Long>) {
        reorderQueue(
            EntryDownloadQueuePolicy.promote(
                queue = queueState.value,
                keys = chapterIds,
                keyOf = { it.chapter.id },
                isActive = {
                    it.status.isActive
                },
            ),
        )
        startDownloads()
    }

    fun removeFromQueue(chapterIds: Collection<Long>) {
        if (chapterIds.isEmpty()) return
        val removesActiveDownload = activeChapterId in chapterIds
        synchronized(queueMutationLock) {
            val removed = _queueState.value.filter { it.chapter.id in chapterIds }
            _queueState.update { current -> current.filterNot { it.chapter.id in chapterIds } }
            store.remove(removed)
        }
        if (removesActiveDownload) activeDownloadJob?.cancel()
        if (queueState.value.isEmpty()) {
            _isRunning.value = false
        }
    }

    fun reorderQueue(downloads: List<BookDownload>) {
        synchronized(queueMutationLock) {
            _queueState.value = EntryDownloadQueuePolicy.reorderPending(
                queue = queueState.value,
                requested = downloads,
                keyOf = { it.chapter.id },
                isActive = {
                    it.status.isActive
                },
            )
            rewriteStoredQueueLocked()
        }
    }

    suspend fun hasPendingDownloads(): Boolean {
        initialized.await()
        return queueState.value.any { it.status == BookDownload.State.QUEUE }
    }

    suspend fun runDownloads() {
        initialized.await()
        if (queueState.value.isEmpty()) return
        processorMutex.lock()
        _isRunning.value = true
        try {
            while (true) {
                val next = queueState.value.firstOrNull { it.status == BookDownload.State.QUEUE } ?: break
                activeChapterId = next.chapter.id
                try {
                    val failure = coroutineScope {
                        val statusObserver = launch(start = CoroutineStart.UNDISPATCHED) {
                            next.statusFlow.drop(1).collect { statusUpdates.emit(next) }
                        }
                        val progressObserver = launch(start = CoroutineStart.UNDISPATCHED) {
                            next.progressFlow.drop(1).collect { progressUpdates.emit(next) }
                        }
                        val job = async { downloader.download(next) }
                        activeDownloadJob = job
                        try {
                            job.await()
                        } finally {
                            if (activeDownloadJob === job) activeDownloadJob = null
                            statusObserver.cancelAndJoin()
                            progressObserver.cancelAndJoin()
                        }
                    }
                    if (failure == null) {
                        synchronized(queueMutationLock) {
                            _queueState.update { current -> current.filterNot { it.chapter.id == next.chapter.id } }
                            store.remove(listOf(next))
                        }
                    } else {
                        next.progress = 0
                        next.failure = failure
                        next.status = BookDownload.State.ERROR
                        statusUpdates.tryEmit(next)
                        reportError(next)
                    }
                } catch (error: CancellationException) {
                    if (queueState.value.any { it.chapter.id == next.chapter.id }) {
                        next.status = BookDownload.State.QUEUE
                        statusUpdates.tryEmit(next)
                        throw error
                    }
                    continue
                } catch (error: Exception) {
                    next.progress = 0
                    next.failure = BookDownloadFailure(BookDownloadFailure.Reason.UNKNOWN, error.message)
                    next.status = BookDownload.State.ERROR
                    statusUpdates.tryEmit(next)
                    reportError(next)
                } finally {
                    activeChapterId = null
                }
            }
        } finally {
            _isRunning.value = false
            processorMutex.unlock()
        }
    }

    private fun reportError(download: BookDownload) {
        _events.tryEmit(
            EntryDownloadEvent.Error(
                entryType = EntryType.BOOK,
                entryIdentity = EntryDownloadEntryIdentity.from(download.entry),
                title = download.entry.title,
                subtitle = download.chapter.name,
                message = download.failure?.toEntryDownloadMessage()
                    ?: EntryDownloadMessage.Resource(MR.strings.download_notifier_unknown_error),
            ),
        )
    }

    suspend fun delete(entry: Entry, chapters: List<EntryChapter>) {
        removeFromQueue(chapters.map(EntryChapter::id))
        cache.ensureInitialized()
        val requestedKeys = chapters.map { chapter ->
            BookDownloadPackageKey(entry.source, entry.url, chapter.url)
        }.toSet()
        val publicationIds = cache.packagesSnapshot()
            .filter { download -> download.manifest.packageKey in requestedKeys }
            .mapNotNull { download -> download.manifest.publicationId }
        val deletedKeys = chapters.mapNotNull { chapter ->
            val packageKey = BookDownloadPackageKey(entry.source, entry.url, chapter.url)
            val directory = cache.packageDirectory(packageKey) ?: return@mapNotNull null
            packageKey.takeIf { directory.delete() || !directory.exists() }
        }
        cache.remove(deletedKeys)
        val remainingPublicationIds = cache.packagesSnapshot().mapNotNull { it.manifest.publicationId }.toSet()
        publicationIds.filterNot(remainingPublicationIds::contains)
            .forEach(::removeDerivedPublicationData)
    }

    suspend fun deleteEntryDownloads(entry: Entry): Boolean {
        removeFromQueue(queueState.value.filter { it.entry.id == entry.id }.map { it.chapter.id })
        cache.ensureInitialized()
        val downloads = cache.packagesSnapshot()
            .filter {
                (it.manifest.sourceId == entry.source && it.manifest.entryUrl == entry.url) ||
                    it.manifest.entryId == entry.id
            }
        val deletedKeys = downloads.mapNotNull { download ->
            download.manifest.packageKey.takeIf {
                val directory = cache.packageDirectory(download.manifest.packageKey) ?: return@takeIf false
                directory.delete() || !directory.exists()
            }
        }
        cache.remove(deletedKeys)
        downloads.mapNotNull { it.manifest.publicationId }
            .forEach(::removeDerivedPublicationData)
        return deletedKeys.size == downloads.size
    }

    private fun removeDerivedPublicationData(publicationId: String) {
        preparedDocumentCache.removePublication(publicationId)
        resourceGatewayFactory.removePublication(publicationId)
    }

    fun invalidateCache() {
        scope.launch { cache.refresh(reportInitialization = true) }
    }

    fun renameSource(oldSource: UnifiedSource, newSource: UnifiedSource) {
        scope.launch {
            cache.ensureInitialized()
            val affectedKeys = cache.packagesSnapshot()
                .filter { it.manifest.sourceId == oldSource.id }
                .map { it.manifest.packageKey }
            if (provider.renameSource(oldSource.name, newSource.name)) {
                cache.replace(affectedKeys, provider.scanSourcePackages(newSource.name).packages)
            }
        }
    }

    suspend fun renameEntry(entry: Entry, newTitle: String) {
        removeFromQueue(queueState.value.filter { it.entry.id == entry.id }.map { it.chapter.id })
        cache.ensureInitialized()
        val sourceName = sourceManager.get(entry.source)?.name ?: return
        val affectedKeys = cache.packagesSnapshot()
            .filter {
                (it.manifest.sourceId == entry.source && it.manifest.entryUrl == entry.url) ||
                    it.manifest.entryId == entry.id
            }
            .map { it.manifest.packageKey }
        if (provider.renameEntry(sourceName, entry, newTitle)) {
            cache.replace(
                affectedKeys,
                provider.scanEntryPackages(sourceName, entry.copy(title = newTitle)).packages,
            )
        }
    }

    fun statusFlow(): Flow<BookDownload> = statusUpdates.asSharedFlow().onStart {
        queueState.value
            .filter { it.status != BookDownload.State.QUEUE }
            .forEach { emit(it) }
    }

    fun progressFlow(): Flow<BookDownload> = progressUpdates.asSharedFlow().onStart {
        queueState.value
            .filter { it.status == BookDownload.State.DOWNLOADING }
            .forEach { emit(it) }
    }

    private fun mergeRestoredQueue(restored: List<BookDownload>) {
        synchronized(queueMutationLock) {
            _queueState.value = mergeRestoredBookDownloads(restored, _queueState.value)
            rewriteStoredQueueLocked()
        }
    }

    private fun rewriteStoredQueueLocked() {
        store.replace(_queueState.value)
    }

    private companion object {
        const val UPDATE_BUFFER_CAPACITY = 128
    }
}

internal fun mergeRestoredBookDownloads(
    restored: List<BookDownload>,
    current: List<BookDownload>,
): List<BookDownload> {
    val currentIds = current.mapTo(mutableSetOf()) { it.chapter.id }
    return restored.filterNot { it.chapter.id in currentIds } + current
}

internal fun List<EntryChapter>.toQueuedBookDownloads(entry: Entry): List<BookDownload> = map { chapter ->
    BookDownload(entry, chapter).apply { status = BookDownload.State.QUEUE }
}
