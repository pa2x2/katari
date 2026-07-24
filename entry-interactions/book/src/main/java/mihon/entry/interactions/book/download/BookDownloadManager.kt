package mihon.entry.interactions.book.download

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import mihon.entry.interactions.EntryDownloadEntryIdentity
import mihon.entry.interactions.EntryDownloadEvent
import mihon.entry.interactions.EntryDownloadMessage
import mihon.entry.interactions.EntryDownloadQueuePolicy
import mihon.entry.interactions.EntryDownloadWorkController
import mihon.entry.interactions.book.download.model.BookDownload
import mihon.entry.interactions.book.download.model.BookDownloadFailure
import mihon.entry.interactions.book.toEntryDownloadMessage
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class BookDownloadManager(
    context: Context,
    private val cache: BookDownloadCache = Injekt.get(),
    private val provider: BookDownloadProvider = Injekt.get(),
    private val downloader: BookDownloader = Injekt.get(),
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
    private val _isRunning = MutableStateFlow(false)
    val isRunning = _isRunning.asStateFlow()
    private val _events = MutableSharedFlow<EntryDownloadEvent>(replay = 16, extraBufferCapacity = 16)
    val events = _events.asSharedFlow()

    @Volatile
    private var activeChapterId: Long? = null

    @Volatile
    private var activeDownloadJob: Job? = null

    init {
        scope.launch {
            try {
                cache.ensureInitialized()
                mergeRestoredQueue(store.restore())
            } finally {
                initialized.complete(Unit)
            }
        }
    }

    fun startDownloads() {
        if (queueState.value.isEmpty()) return
        queueState.value.forEach { download ->
            if (download.status != BookDownload.State.RESOLVING && download.status != BookDownload.State.DOWNLOADING) {
                download.failure = null
                download.status = BookDownload.State.QUEUE
            }
        }
        rewriteStoredQueue()
        if (!_isRunning.value) workController.start()
    }

    fun pauseDownloads() {
        val hasQueuedDownloads = synchronized(queueMutationLock) {
            if (_queueState.value.isEmpty()) {
                false
            } else {
                _queueState.value
                    .filter {
                        it.status == BookDownload.State.RESOLVING ||
                            it.status == BookDownload.State.DOWNLOADING
                    }
                    .forEach { it.status = BookDownload.State.QUEUE }
                _isRunning.value = false
                rewriteStoredQueueLocked()
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
            .toQueuedBookDownloads(entry)
        synchronized(queueMutationLock) {
            val chapterIds = chapters.mapTo(mutableSetOf(), EntryChapter::id)
            _queueState.update { current -> current.filterNot { it.chapter.id in chapterIds } + queued }
            rewriteStoredQueueLocked()
        }
        if (autoStart) startDownloads()
    }

    fun startDownloadsNow(chapterIds: Collection<Long>) {
        reorderQueue(
            EntryDownloadQueuePolicy.promote(
                queue = queueState.value,
                keys = chapterIds,
                keyOf = { it.chapter.id },
                isActive = {
                    it.status == BookDownload.State.RESOLVING ||
                        it.status == BookDownload.State.DOWNLOADING
                },
            ),
        )
        startDownloads()
    }

    fun removeFromQueue(chapterIds: Collection<Long>) {
        if (chapterIds.isEmpty()) return
        val removesActiveDownload = activeChapterId in chapterIds
        synchronized(queueMutationLock) {
            _queueState.update { current -> current.filterNot { it.chapter.id in chapterIds } }
            rewriteStoredQueueLocked()
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
                    it.status == BookDownload.State.RESOLVING ||
                        it.status == BookDownload.State.DOWNLOADING
                },
            )
            rewriteStoredQueueLocked()
        }
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
                        val job = async { downloader.download(next) }
                        activeDownloadJob = job
                        try {
                            job.await()
                        } finally {
                            if (activeDownloadJob === job) activeDownloadJob = null
                        }
                    }
                    if (failure == null) {
                        synchronized(queueMutationLock) {
                            _queueState.update { current -> current.filterNot { it.chapter.id == next.chapter.id } }
                            rewriteStoredQueueLocked()
                        }
                    } else {
                        next.progress = 0
                        next.failure = failure
                        next.status = BookDownload.State.ERROR
                        rewriteStoredQueue()
                        reportError(next)
                    }
                } catch (error: CancellationException) {
                    if (queueState.value.any { it.chapter.id == next.chapter.id }) {
                        next.status = BookDownload.State.QUEUE
                        rewriteStoredQueue()
                        throw error
                    }
                    continue
                } catch (error: Exception) {
                    next.progress = 0
                    next.failure = BookDownloadFailure(BookDownloadFailure.Reason.UNKNOWN, error.message)
                    next.status = BookDownload.State.ERROR
                    rewriteStoredQueue()
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
                    ?: EntryDownloadMessage.Resource(tachiyomi.i18n.MR.strings.download_notifier_unknown_error),
            ),
        )
    }

    suspend fun delete(entry: Entry, chapters: List<EntryChapter>) {
        removeFromQueue(chapters.map(EntryChapter::id))
        cache.ensureInitialized()
        val deletedKeys = chapters.mapNotNull { chapter ->
            val packageKey = BookDownloadPackageKey(entry.source, entry.url, chapter.url)
            val directory = cache.get(packageKey)?.directory ?: return@mapNotNull null
            packageKey.takeIf { directory.delete() || !directory.exists() }
        }
        cache.remove(deletedKeys)
    }

    suspend fun deleteEntryDownloads(entry: Entry): Boolean {
        removeFromQueue(queueState.value.filter { it.entry.id == entry.id }.map { it.chapter.id })
        cache.ensureInitialized()
        val downloads = cache.packages.value.values
            .filter {
                (it.manifest.sourceId == entry.source && it.manifest.entryUrl == entry.url) ||
                    it.manifest.entryId == entry.id
            }
        val deletedKeys = downloads.mapNotNull { download ->
            download.manifest.packageKey.takeIf {
                download.directory.delete() || !download.directory.exists()
            }
        }
        cache.remove(deletedKeys)
        return deletedKeys.size == downloads.size
    }

    fun invalidateCache() {
        scope.launch { cache.refresh(reportInitialization = true) }
    }

    fun renameSource(oldSource: UnifiedSource, newSource: UnifiedSource) {
        scope.launch {
            cache.ensureInitialized()
            val affectedKeys = cache.packages.value.values
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
        val affectedKeys = cache.packages.value.values
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

    @OptIn(ExperimentalCoroutinesApi::class)
    fun statusFlow(): Flow<BookDownload> = queueState.flatMapLatest { downloads ->
        downloads.map { download -> download.statusFlow.drop(1).map { download } }.merge()
    }.onStart {
        emitAll(queueState.value.filter { it.status == BookDownload.State.DOWNLOADING }.asFlow())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun progressFlow(): Flow<BookDownload> = queueState.flatMapLatest { downloads ->
        downloads.map { download -> download.progressFlow.drop(1).map { download } }.merge()
    }.onStart {
        emitAll(queueState.value.filter { it.status == BookDownload.State.DOWNLOADING }.asFlow())
    }

    private fun mergeRestoredQueue(restored: List<BookDownload>) {
        synchronized(queueMutationLock) {
            _queueState.value = mergeRestoredBookDownloads(restored, _queueState.value)
            rewriteStoredQueueLocked()
        }
    }

    private fun rewriteStoredQueue() = synchronized(queueMutationLock) { rewriteStoredQueueLocked() }

    private fun rewriteStoredQueueLocked() {
        store.replace(_queueState.value)
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
