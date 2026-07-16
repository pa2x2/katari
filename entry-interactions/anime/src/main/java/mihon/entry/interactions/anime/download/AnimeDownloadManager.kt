package mihon.entry.interactions.anime.download
import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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
import mihon.entry.interactions.EntryDownloadEvent
import mihon.entry.interactions.EntryDownloadMessage
import mihon.entry.interactions.anime.download.model.AnimeDownload
import mihon.entry.interactions.anime.download.model.AnimeDownloadFailure
import mihon.entry.interactions.anime.toEntryDownloadMessage
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
    private val queueMutationLock = Any()

    @Volatile
    private var activeDownload: ActiveDownload? = null

    init {
        scope.launch {
            val downloads = store.restore()
            if (downloads.isNotEmpty()) {
                mergeRestoredQueue(downloads)
            } else {
                synchronized(queueMutationLock) {
                    rewriteStoredQueue()
                }
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
        _isRunning.value = true
        launchProcessorIfNeeded()
    }

    fun pauseDownloads() {
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

        val wasRunning = _isRunning.value
        val removesActiveDownload = isActiveEpisodeBeingRemoved(activeDownload?.episodeId, episodeIds)
        if (wasRunning && removesActiveDownload) {
            processorJob?.cancel()
            processorJob = null
        }

        synchronized(queueMutationLock) {
            _queueState.update { current -> current.filterNot { it.episode.id in episodeIds } }
            store.removeAll(toRemove)
        }

        if (queueState.value.isEmpty()) {
            _isRunning.value = false
        } else if (wasRunning && removesActiveDownload) {
            queueState.value
                .filter { it.status == AnimeDownload.State.RESOLVING || it.status == AnimeDownload.State.DOWNLOADING }
                .forEach { it.status = AnimeDownload.State.QUEUE }
            launchProcessorIfNeeded()
        }
    }

    fun reorderQueue(downloads: List<AnimeDownload>) {
        updateQueue(downloads)
    }

    suspend fun deleteEpisodes(anime: Entry, episodes: List<EntryChapter>) {
        if (episodes.isEmpty()) return
        removeFromQueue(episodes.map(EntryChapter::id))
        val source = sourceManager.get(anime.source) ?: return
        val (_, episodeDirs) = provider.findEpisodeDirs(episodes, anime, source)
        episodeDirs.forEach { it.delete() }
        cache.removeEpisodes(episodes, anime)
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

    private fun launchProcessorIfNeeded() {
        if (processorJob?.isActive == true) return
        val processorToken = Any()
        processorJob = scope.launch {
            while (_isRunning.value) {
                val next = queueState.value.firstOrNull { it.status == AnimeDownload.State.QUEUE } ?: break
                try {
                    activeDownload = ActiveDownload(next.episode.id, processorToken)
                    val failure = downloader.download(next)
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
                } catch (e: Throwable) {
                    if (e is CancellationException) throw e
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
            _isRunning.value = false
            processorJob = null
        }
    }

    private fun reportError(download: AnimeDownload) {
        _events.tryEmit(
            EntryDownloadEvent.Error(
                entryType = EntryType.ANIME,
                entryId = download.anime.id,
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
