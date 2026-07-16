package mihon.entry.interactions.book.download

import android.content.Context
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
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
import mihon.entry.interactions.book.download.model.BookDownload
import mihon.entry.interactions.book.download.model.BookDownloadFailure
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
    private val notifier: BookDownloadNotifier = BookDownloadNotifier(context.applicationContext),
    private val workController: BookDownloadWorkController = DefaultBookDownloadWorkController,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val queueMutationLock = Any()
    private val processorMutex = Mutex()
    private val initialized = CompletableDeferred<Unit>()
    private val _queueState = MutableStateFlow<List<BookDownload>>(emptyList())
    val queueState = _queueState.asStateFlow()
    val cacheChanges = cache.changes
    private val _isRunning = MutableStateFlow(false)
    val isRunning = _isRunning.asStateFlow()

    @Volatile
    private var activeChapterId: Long? = null

    @Volatile
    private var pauseRequested = false

    init {
        scope.launch {
            try {
                cache.ensureInitialized()
                mergeRestoredQueue(store.restore())
            } finally {
                initialized.complete(Unit)
            }
        }
        scope.launch {
            progressFlow().collect { download ->
                if (
                    download.status == BookDownload.State.RESOLVING ||
                    download.status == BookDownload.State.DOWNLOADING
                ) {
                    notifier.onProgressChange(download)
                }
            }
        }
    }

    fun startDownloads() {
        if (queueState.value.isEmpty()) return
        pauseRequested = false
        queueState.value.forEach { download ->
            if (download.status != BookDownload.State.RESOLVING && download.status != BookDownload.State.DOWNLOADING) {
                download.failure = null
                download.status = BookDownload.State.QUEUE
            }
        }
        rewriteStoredQueue()
        if (!_isRunning.value) workController.start(appContext)
    }

    fun pauseDownloads() {
        val hasQueuedDownloads = synchronized(queueMutationLock) {
            if (_queueState.value.isEmpty()) {
                pauseRequested = false
                false
            } else {
                pauseRequested = true
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
        workController.stop(appContext)
        synchronized(queueMutationLock) {
            if (hasQueuedDownloads && _queueState.value.isNotEmpty()) {
                notifier.onPaused()
            } else {
                pauseRequested = false
                _isRunning.value = false
                notifier.onComplete()
            }
        }
    }

    fun clearQueue() {
        workController.stop(appContext)
        synchronized(queueMutationLock) {
            _queueState.value = emptyList()
            store.clear()
        }
        pauseRequested = false
        _isRunning.value = false
        notifier.onComplete()
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

    fun startDownloadNow(chapterId: Long) {
        val selected = queueState.value.firstOrNull { it.chapter.id == chapterId } ?: return
        reorderQueue(listOf(selected) + queueState.value.filterNot { it.chapter.id == chapterId })
        startDownloads()
    }

    fun removeFromQueue(chapterIds: Collection<Long>) {
        if (chapterIds.isEmpty()) return
        val wasRunning = _isRunning.value
        val removesActiveDownload = activeChapterId in chapterIds
        if (removesActiveDownload) workController.stop(appContext)
        synchronized(queueMutationLock) {
            _queueState.update { current -> current.filterNot { it.chapter.id in chapterIds } }
            rewriteStoredQueueLocked()
        }
        if (queueState.value.isEmpty()) {
            _isRunning.value = false
            notifier.onComplete()
        } else if (wasRunning && removesActiveDownload) {
            _isRunning.value = false
            startDownloads()
        }
    }

    fun reorderQueue(downloads: List<BookDownload>) {
        synchronized(queueMutationLock) {
            _queueState.value = downloads
            rewriteStoredQueueLocked()
        }
    }

    suspend fun runDownloads() {
        initialized.await()
        processorMutex.lock()
        _isRunning.value = true
        try {
            while (true) {
                val next = queueState.value.firstOrNull { it.status == BookDownload.State.QUEUE } ?: break
                activeChapterId = next.chapter.id
                notifier.onProgressChange(next)
                try {
                    val failure = downloader.download(next)
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
                        notifier.onError(next)
                    }
                } catch (error: CancellationException) {
                    next.status = BookDownload.State.QUEUE
                    rewriteStoredQueue()
                    throw error
                } catch (error: Exception) {
                    next.progress = 0
                    next.failure = BookDownloadFailure(BookDownloadFailure.Reason.UNKNOWN, error.message)
                    next.status = BookDownload.State.ERROR
                    rewriteStoredQueue()
                    notifier.onError(next)
                } finally {
                    activeChapterId = null
                }
            }
        } finally {
            _isRunning.value = false
            if (!pauseRequested || queueState.value.isEmpty()) notifier.onComplete()
            processorMutex.unlock()
        }
    }

    suspend fun delete(entry: Entry, chapters: List<EntryChapter>) {
        removeFromQueue(chapters.map(EntryChapter::id))
        cache.ensureInitialized()
        chapters.forEach { chapter ->
            cache.get(BookDownloadPackageKey(entry.source, entry.url, chapter.url))?.directory?.delete()
        }
        cache.refresh()
    }

    suspend fun deleteEntryDownloads(entry: Entry, memberEntryIds: Set<Long> = setOf(entry.id)) {
        removeFromQueue(queueState.value.filter { it.entry.id in memberEntryIds }.map { it.chapter.id })
        cache.ensureInitialized()
        cache.packages.value.values
            .filter {
                (it.manifest.sourceId == entry.source && it.manifest.entryUrl == entry.url) ||
                    it.manifest.entryId in memberEntryIds
            }
            .forEach { it.directory.delete() }
        cache.refresh()
    }

    fun invalidateCache() {
        scope.launch { cache.refresh(reportInitialization = true) }
    }

    fun renameSource(oldSource: UnifiedSource, newSource: UnifiedSource) {
        scope.launch {
            provider.renameSource(oldSource.name, newSource.name)
            cache.refresh()
        }
    }

    suspend fun renameEntry(entry: Entry, newTitle: String) {
        removeFromQueue(queueState.value.filter { it.entry.id == entry.id }.map { it.chapter.id })
        val sourceName = sourceManager.get(entry.source)?.name ?: return
        provider.renameEntry(sourceName, entry, newTitle)
        cache.refresh()
    }

    fun statusFlow(): Flow<BookDownload> = queueState.flatMapLatest { downloads ->
        downloads.map { download -> download.statusFlow.drop(1).map { download } }.merge()
    }.onStart {
        emitAll(queueState.value.filter { it.status == BookDownload.State.DOWNLOADING }.asFlow())
    }

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
