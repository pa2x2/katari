package mihon.entry.interactions.anime.download
import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
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
import mihon.entry.interactions.EntryDownloadEntryIdentity
import mihon.entry.interactions.EntryDownloadEvent
import mihon.entry.interactions.EntryDownloadMessage
import mihon.entry.interactions.EntryDownloadQueuePolicy
import mihon.entry.interactions.EntryDownloadWorkController
import mihon.entry.interactions.anime.download.model.AnimeDownload
import mihon.entry.interactions.anime.download.model.AnimeDownloadFailure
import mihon.entry.interactions.anime.toEntryDownloadMessage
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.entry.model.DownloadPreferences
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class AnimeDownloadManager(
    context: Context,
    private val cache: AnimeDownloadCache = AnimeDownloadCache(context),
    private val provider: AnimeDownloadProvider = AnimeDownloadProvider(context),
    private val downloader: AnimeDownloader = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val store: AnimeDownloadStore = AnimeDownloadStore(context),
    private val workController: EntryDownloadWorkController = Injekt.get(),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _queueState = MutableStateFlow<List<AnimeDownload>>(emptyList())
    val queueState = _queueState.asStateFlow()
    val cacheChanges = cache.changes

    private val _isRunning = MutableStateFlow(false)
    val isRunning = _isRunning.asStateFlow()

    private val _events = MutableSharedFlow<EntryDownloadEvent>(replay = 16, extraBufferCapacity = 16)
    val events = _events.asSharedFlow()

    private var processorJob: Job? = null
    private var activeDownloadJob: Job? = null
    private val queueMutationLock = Any()
    private val initialized = CompletableDeferred<Unit>()

    @Volatile
    private var activeDownload: ActiveDownload? = null

    init {
        scope.launch {
            try {
                val downloads = store.restore()
                if (downloads.isNotEmpty()) {
                    mergeRestoredQueue(downloads)
                } else {
                    synchronized(queueMutationLock) {
                        rewriteStoredQueue()
                    }
                }
            } finally {
                initialized.complete(Unit)
            }
        }
    }

    fun startDownloads() {
        if (queueState.value.isEmpty()) return
        queueState.value.forEach { download ->
            if (
                download.status != AnimeDownload.State.DOWNLOADED &&
                download.status != AnimeDownload.State.RESOLVING &&
                download.status != AnimeDownload.State.DOWNLOADING
            ) {
                download.status = AnimeDownload.State.QUEUE
                download.failure = null
            }
        }
        if (!_isRunning.value) workController.start()
    }

    fun startDownloadsNow(episodeIds: Collection<Long>) {
        reorderQueue(
            EntryDownloadQueuePolicy.promote(
                queue = queueState.value,
                keys = episodeIds,
                keyOf = { it.episode.id },
                isActive = {
                    it.status == AnimeDownload.State.RESOLVING ||
                        it.status == AnimeDownload.State.DOWNLOADING
                },
            ),
        )
        startDownloads()
    }

    fun pauseDownloads() {
        workController.stop()
        processorJob?.cancel()
        processorJob = null
        if (queueState.value.isEmpty()) {
            _isRunning.value = false
            return
        }
        queueState.value
            .filter { it.status == AnimeDownload.State.RESOLVING || it.status == AnimeDownload.State.DOWNLOADING }
            .forEach { it.status = AnimeDownload.State.QUEUE }
        _isRunning.value = false
    }

    fun clearQueue() {
        workController.stop()
        synchronized(queueMutationLock) {
            _queueState.value = emptyList()
            processorJob?.cancel()
            processorJob = null
            _isRunning.value = false
            store.clear()
        }
    }

    fun queueEpisodes(
        anime: Entry,
        episodes: List<EntryChapter>,
        preferences: DownloadPreferences,
        autoStart: Boolean = true,
    ) {
        if (episodes.isEmpty()) return

        val newDownloads = episodes
            .sortedByDescending { it.sourceOrder }
            .map {
                AnimeDownload(
                    anime = anime,
                    episode = it,
                    preferences = preferences.copy(entryId = anime.id),
                )
            }

        if (newDownloads.isEmpty()) {
            if (autoStart) {
                startDownloads()
            }
            return
        }

        synchronized(queueMutationLock) {
            val episodeIds = episodes.map(EntryChapter::id).toSet()
            _queueState.update { current ->
                current.filterNot { it.episode.id in episodeIds } + newDownloads
            }
            rewriteStoredQueue()
        }
        if (autoStart) {
            startDownloads()
        }
    }

    fun removeFromQueue(episodeIds: Collection<Long>) {
        if (episodeIds.isEmpty()) return
        val toRemove = queueState.value.filter { it.episode.id in episodeIds }
        if (toRemove.isEmpty()) return

        val removesActiveDownload = isActiveEpisodeBeingRemoved(activeDownload?.episodeId, episodeIds)
        synchronized(queueMutationLock) {
            _queueState.update { current -> current.filterNot { it.episode.id in episodeIds } }
            store.removeAll(toRemove)
        }
        if (removesActiveDownload) activeDownloadJob?.cancel()

        if (queueState.value.isEmpty()) {
            _isRunning.value = false
        }
    }

    fun reorderQueue(downloads: List<AnimeDownload>) {
        updateQueue(
            EntryDownloadQueuePolicy.reorderPending(
                queue = queueState.value,
                requested = downloads,
                keyOf = { it.episode.id },
                isActive = {
                    it.status == AnimeDownload.State.RESOLVING ||
                        it.status == AnimeDownload.State.DOWNLOADING
                },
            ),
        )
    }

    suspend fun deleteEpisodes(anime: Entry, episodes: List<EntryChapter>) {
        withIOContext {
            if (episodes.isEmpty()) return@withIOContext
            removeFromQueue(episodes.map(EntryChapter::id))
            sourceManager.get(anime.source)?.let { source ->
                val (_, episodeDirs) = provider.findEpisodeDirs(episodes, anime, source)
                episodeDirs.forEach { it.delete() }
            }
            cache.removeEpisodes(episodes, anime)
        }
    }

    suspend fun deleteAnime(anime: Entry): Boolean {
        return withIOContext {
            val queuedEpisodeIds = queueState.value
                .filter { it.anime.id == anime.id }
                .map { it.episode.id }
            removeFromQueue(queuedEpisodeIds)
            val source = sourceManager.get(anime.source)
            val animeDirectory = source?.let { provider.findAnimeDir(anime.title, it) }
            val removed = animeDirectory == null || animeDirectory.delete() || !animeDirectory.exists()
            if (!removed) return@withIOContext false
            cache.removeAnime(anime)
            true
        }
    }

    fun renameSource(oldSource: UnifiedSource, newSource: UnifiedSource) {
        if (provider.renameSource(oldSource, newSource)) cache.invalidateCache()
    }

    suspend fun renameAnime(anime: Entry, newTitle: String) {
        withIOContext {
            val source = sourceManager.get(anime.source) ?: return@withIOContext
            val queuedEpisodeIds = queueState.value
                .filter { it.anime.id == anime.id }
                .map { it.episode.id }
            removeFromQueue(queuedEpisodeIds)
            if (provider.renameEntry(source, anime, newTitle)) cache.invalidateCache()
        }
    }

    fun isEpisodeDownloaded(
        episodeName: String,
        episodeUrl: String,
        animeTitle: String,
        sourceId: Long,
        skipCache: Boolean = false,
    ): Boolean {
        return cache.isEpisodeDownloaded(episodeName, episodeUrl, animeTitle, sourceId, skipCache)
    }

    fun getDownloadCount(anime: Entry): Int {
        return cache.getDownloadCount(anime)
    }

    fun getTotalDownloadCount(): Int {
        return cache.getTotalDownloadCount()
    }

    fun statusFlow(): Flow<AnimeDownload> = queueState
        .flatMapLatest { downloads ->
            downloads
                .map { download ->
                    download.statusFlow.drop(1).map { download }
                }
                .merge()
        }
        .onStart {
            emitAll(
                queueState.value.filter { download -> download.status == AnimeDownload.State.DOWNLOADING }.asFlow(),
            )
        }

    fun progressFlow(): Flow<AnimeDownload> = queueState
        .flatMapLatest { downloads ->
            downloads
                .map { download ->
                    download.progressFlow.drop(1).map { download }
                }
                .merge()
        }
        .onStart {
            emitAll(
                queueState.value.filter { download -> download.status == AnimeDownload.State.DOWNLOADING }.asFlow(),
            )
        }

    private fun updateQueue(downloads: List<AnimeDownload>) {
        synchronized(queueMutationLock) {
            _queueState.value = downloads
            rewriteStoredQueue()
        }
    }

    private fun mergeRestoredQueue(restored: List<AnimeDownload>) {
        synchronized(queueMutationLock) {
            val current = _queueState.value
            _queueState.value = mergeRestoredDownloads(restored, current)
            rewriteStoredQueue()
        }
    }

    private fun rewriteStoredQueue() {
        store.clear()
        store.addAll(_queueState.value)
    }

    suspend fun runDownloadsUntilIdle() {
        initialized.await()
        if (queueState.value.isEmpty()) return
        queueState.value.forEach { download ->
            if (
                download.status != AnimeDownload.State.DOWNLOADED &&
                download.status != AnimeDownload.State.RESOLVING &&
                download.status != AnimeDownload.State.DOWNLOADING
            ) {
                download.status = AnimeDownload.State.QUEUE
                download.failure = null
            }
        }
        val processorToken = Any()
        processorJob = currentCoroutineContext()[Job]
        _isRunning.value = true
        try {
            while (_isRunning.value) {
                val next = queueState.value.firstOrNull { it.status == AnimeDownload.State.QUEUE } ?: break
                try {
                    activeDownload = ActiveDownload(next.episode.id, processorToken)
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
                            _queueState.update { current -> current.filterNot { it.episode.id == next.episode.id } }
                            store.remove(next)
                        }
                    } else {
                        next.progress = 0
                        next.failure = failure
                        next.status = AnimeDownload.State.ERROR
                        reportError(next)
                    }
                } catch (e: CancellationException) {
                    if (queueState.value.any { it.episode.id == next.episode.id }) {
                        next.status = AnimeDownload.State.QUEUE
                        rewriteStoredQueue()
                        throw e
                    }
                    continue
                } catch (e: Throwable) {
                    next.progress = 0
                    next.failure = AnimeDownloadFailure(
                        reason = AnimeDownloadFailure.Reason.UNKNOWN,
                        message = e.message,
                    )
                    next.status = AnimeDownload.State.ERROR
                    reportError(next)
                } finally {
                    if (activeDownload?.processorToken === processorToken) {
                        activeDownload = null
                    }
                }
            }
        } finally {
            _isRunning.value = false
            if (activeDownload?.processorToken === processorToken) activeDownload = null
            if (processorJob === currentCoroutineContext()[Job]) processorJob = null
        }
    }

    private fun reportError(download: AnimeDownload) {
        _events.tryEmit(
            EntryDownloadEvent.Error(
                entryType = EntryType.ANIME,
                entryIdentity = EntryDownloadEntryIdentity.from(download.anime),
                title = download.anime.title,
                subtitle = download.episode.name,
                message = download.failure?.toEntryDownloadMessage()
                    ?: EntryDownloadMessage.Resource(tachiyomi.i18n.MR.strings.download_notifier_unknown_error),
            ),
        )
    }

    companion object {
        const val TMP_DIR_SUFFIX = "_tmp"
    }

    private data class ActiveDownload(
        val episodeId: Long,
        val processorToken: Any,
    )
}

internal fun isActiveEpisodeBeingRemoved(activeEpisodeId: Long?, episodeIds: Collection<Long>): Boolean {
    return activeEpisodeId != null && activeEpisodeId in episodeIds
}

internal fun mergeRestoredDownloads(
    restored: List<AnimeDownload>,
    current: List<AnimeDownload>,
): List<AnimeDownload> {
    val currentEpisodeIds = current.mapTo(mutableSetOf()) { it.episode.id }
    return restored.filterNot { it.episode.id in currentEpisodeIds } + current
}
