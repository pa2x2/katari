package mihon.entry.interactions.anime

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import eu.kanade.tachiyomi.source.entry.SubtitleSource
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import mihon.entry.interactions.EntryBulkDownloadAction
import mihon.entry.interactions.EntryBulkDownloadActionType
import mihon.entry.interactions.EntryBulkDownloadCandidateResult
import mihon.entry.interactions.EntryDownloadOption
import mihon.entry.interactions.EntryDownloadOptionGroup
import mihon.entry.interactions.EntryDownloadOptionSelection
import mihon.entry.interactions.EntryDownloadOptions
import mihon.entry.interactions.EntryDownloadProcessor
import mihon.entry.interactions.EntryDownloadQueueGroup
import mihon.entry.interactions.EntryDownloadQueueItem
import mihon.entry.interactions.EntryDownloadState
import mihon.entry.interactions.EntryDownloadStatus
import mihon.entry.interactions.anime.download.model.AnimeDownload
import mihon.entry.interactions.anime.download.streamChoiceKey
import mihon.entry.interactions.anime.download.subtitleChoiceKey
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.entry.model.DownloadPreferences
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.VideoDownloadQualityMode
import tachiyomi.domain.entry.service.sortedForReading
import tachiyomi.i18n.MR

internal class AnimeDownloadProcessor(
    private val dependencies: AnimeEntryInteractionRuntimeDependencies,
) : EntryDownloadProcessor {
    private val animeDownloadManager = dependencies.animeDownloadManager

    override val type: EntryType = EntryType.ANIME
    override val changes: Flow<Unit> = combine(
        animeDownloadManager.cacheChanges,
        animeDownloadManager.queueState.map { Unit },
    ) { _, _ -> }
    override val isInitializing: Flow<Boolean> = dependencies.animeDownloadCache.isInitializing
    override val isRunning: Flow<Boolean> = animeDownloadManager.isRunning
    override val queueState: Flow<List<EntryDownloadQueueGroup>> = animeDownloadManager.queueState
        .map { downloads -> downloads.toAnimeEntryDownloadQueueGroups(dependencies.sourceManager) }
        .map { groups -> groups.map { it.requireAnime() } }
    override val events = animeDownloadManager.events

    override fun updates(): Flow<EntryDownloadStatus> {
        return merge(
            animeDownloadManager.statusFlow().map { it.toEntryDownloadStatus() },
            animeDownloadManager.progressFlow().map { it.toEntryDownloadStatus() },
        ).map { it.requireAnime() }
    }

    override fun queueStatusUpdates(): Flow<EntryDownloadQueueItem> {
        return animeDownloadManager.statusFlow()
            .map { download -> download.toEntryDownloadQueueItem().requireAnime() }
    }

    override fun queueProgressUpdates(): Flow<EntryDownloadQueueItem> {
        return animeDownloadManager.progressFlow()
            .map { download -> download.toEntryDownloadQueueItem().requireAnime() }
    }

    override fun startDownloads() {
        animeDownloadManager.startDownloads()
    }

    override fun pauseDownloads() {
        animeDownloadManager.pauseDownloads()
    }

    override fun clearQueue() {
        animeDownloadManager.clearQueue()
    }

    override fun invalidateCache() {
        dependencies.animeDownloadCache.invalidateCache()
    }

    override fun renameSource(oldSource: UnifiedSource, newSource: UnifiedSource) {
        // Anime download directories are keyed by source id, so a source display-name rename does not move files.
    }

    override fun reorderQueue(items: List<EntryDownloadQueueItem>) {
        items.requireAnime()
        animeDownloadManager.reorderQueue(
            items.mapNotNull { item ->
                animeDownloadManager.queueState.value.firstOrNull { it.episode.id == item.childId }
            },
        )
    }

    override fun reorderSeries(entryId: Long, moveToTop: Boolean) {
        val (series, others) = animeDownloadManager.queueState.value.partition { it.anime.id == entryId }
        animeDownloadManager.reorderQueue(if (moveToTop) series + others else others + series)
    }

    override fun cancelQueuedDownloads(items: List<EntryDownloadQueueItem>) {
        items.requireAnime()
        val episodeIds = items.map { it.childId }
        if (episodeIds.isNotEmpty()) {
            animeDownloadManager.removeFromQueue(episodeIds)
        }
    }

    override suspend fun queue(entry: Entry, chapters: List<EntryChapter>, autoStart: Boolean) {
        entry.requireAnime()
        animeDownloadManager.queueEpisodes(
            anime = entry,
            episodes = chapters,
            preferences = downloadPreferences(entry),
            autoStart = autoStart,
        )
    }

    override suspend fun download(entry: Entry, chapters: List<EntryChapter>, startNow: Boolean) {
        entry.requireAnime()
        animeDownloadManager.queueEpisodes(
            anime = entry,
            episodes = chapters,
            preferences = downloadPreferences(entry),
            autoStart = startNow,
        )
    }

    override suspend fun downloadWithOptions(
        entry: Entry,
        chapters: List<EntryChapter>,
        selection: EntryDownloadOptionSelection,
        startNow: Boolean,
    ) {
        entry.requireAnime()
        val currentPreferences = downloadPreferences(entry)
        val persistedPreferences = currentPreferences.copy(
            entryId = entry.id,
            dubKey = selection.values.selectedValueOr(OPTION_DUB, currentPreferences.dubKey),
            streamKey = selection.values.selectedValueOr(OPTION_STREAM, currentPreferences.streamKey),
            subtitleKey = selection.values.selectedValueOr(OPTION_SUBTITLE, currentPreferences.subtitleKey),
            qualityMode = selection.values[OPTION_QUALITY]
                ?.let { runCatching { VideoDownloadQualityMode.valueOf(it) }.getOrNull() }
                ?: currentPreferences.qualityMode,
            updatedAt = System.currentTimeMillis(),
        )
        dependencies.downloadPreferencesRepository.upsert(persistedPreferences)
        animeDownloadManager.queueEpisodes(
            anime = entry,
            episodes = chapters,
            preferences = persistedPreferences,
            autoStart = startNow,
        )
    }

    override fun supportsDownloadOptions(entry: Entry): Boolean {
        entry.requireAnime()
        return true
    }

    override suspend fun resolveDownloadOptions(
        context: Context,
        entry: Entry,
        chapter: EntryChapter,
    ): EntryDownloadOptions {
        entry.requireAnime()
        val preferences = downloadPreferences(entry)
        val source = dependencies.sourceManager.get(entry.source)
            ?: return entryDownloadOptions(context, preferences)
        val media = runCatching {
            source.getMedia(
                chapter.toSEntryChapter(),
                PlaybackSelection(
                    dubKey = preferences.dubKey,
                    streamKey = preferences.streamKey,
                ),
            )
        }.getOrNull()
        val playback = (media as? EntryMedia.Playback)?.descriptor
            ?: return entryDownloadOptions(context, preferences)
        val subtitles = if (source is SubtitleSource) {
            runCatching { source.getSubtitles(chapter.toSEntryChapter(), playback.selection) }
                .getOrDefault(emptyList())
        } else {
            emptyList()
        }

        return entryDownloadOptions(
            context = context,
            preferences = preferences,
            dubOptions = playback.dubs.map { EntryDownloadOption(it.key, it.label) },
            streamOptions = playback.streams
                .filter { it.request.url.isNotBlank() }
                .map { stream ->
                    val key = streamChoiceKey(stream)
                    EntryDownloadOption(key, stream.label.ifBlank { key })
                }
                .distinctBy(EntryDownloadOption::key),
            subtitleOptions = subtitles
                .filter { it.request.url.isNotBlank() }
                .map { subtitle ->
                    EntryDownloadOption(
                        key = subtitleChoiceKey(subtitle),
                        label = subtitle.language?.takeIf(String::isNotBlank)
                            ?.let { "${subtitle.label} ($it)" }
                            ?: subtitle.label,
                    )
                }
                .distinctBy(EntryDownloadOption::key),
        )
    }

    override fun supportsBulkDownload(entry: Entry): Boolean {
        entry.requireAnime()
        return true
    }

    override suspend fun resolveBulkDownloadCandidates(
        entry: Entry,
        action: EntryBulkDownloadAction,
        candidates: List<EntryChapter>?,
        memberEntryIds: List<Long>,
    ): EntryBulkDownloadCandidateResult {
        entry.requireAnime()
        if (action.type == EntryBulkDownloadActionType.BOOKMARKED) {
            return EntryBulkDownloadCandidateResult.Unsupported
        }
        val chapters = candidates ?: dependencies.entryChapterRepository
            .getChaptersByEntryIdAwait(entry.id, applyScanlatorFilter = true)
            .filterNot { it.read }
            .filterNot { isDownloaded(entry, it) }
        val unconsumed = chapters
            .filterNot { it.read }
            .sortedForReading(entry, memberEntryIds.ifEmpty { chapters.map(EntryChapter::entryId).distinct() })
        return EntryBulkDownloadCandidateResult.Supported(
            chapters = when (action.type) {
                EntryBulkDownloadActionType.NEXT -> action.limit?.let(unconsumed::take) ?: unconsumed
                EntryBulkDownloadActionType.UNREAD -> unconsumed
                EntryBulkDownloadActionType.BOOKMARKED -> error("Handled above")
            },
        )
    }

    override suspend fun filterAutoDownloadCandidates(
        entry: Entry,
        chapters: List<EntryChapter>,
    ): List<EntryChapter> {
        entry.requireAnime()
        return chapters.takeIf { dependencies.downloadPreferences.downloadNewEntryChapters.get() }.orEmpty()
    }

    override suspend fun delete(entry: Entry, chapters: List<EntryChapter>) {
        entry.requireAnime()
        animeDownloadManager.deleteEpisodes(entry, chapters)
    }

    override suspend fun deleteEntryDownloads(entry: Entry) {
        entry.requireAnime()
        val episodes = dependencies.entryChapterRepository.getChaptersByEntryIdAwait(entry.id)
        animeDownloadManager.deleteEpisodes(entry, episodes)
    }

    override fun hasDownloads(entry: Entry): Boolean {
        entry.requireAnime()
        return animeDownloadManager.getDownloadCount(entry) > 0
    }

    override fun getDownloadCount(entry: Entry): Int {
        entry.requireAnime()
        return animeDownloadManager.getDownloadCount(entry)
    }

    override fun getTotalDownloadCount(): Int {
        return animeDownloadManager.getTotalDownloadCount()
    }

    override fun isDownloaded(entry: Entry, chapter: EntryChapter, skipCache: Boolean): Boolean {
        entry.requireAnime()
        return animeDownloadManager.isEpisodeDownloaded(
            episodeName = chapter.name,
            episodeUrl = chapter.url,
            animeTitle = entry.title,
            sourceId = entry.source,
            skipCache = skipCache,
        )
    }

    override fun getStatus(
        chapterId: Long,
        chapterName: String,
        chapterScanlator: String?,
        chapterUrl: String,
        entryTitle: String,
        sourceId: Long,
    ): EntryDownloadStatus {
        return animeDownloadManager.queueState.value
            .firstOrNull { it.episode.id == chapterId }
            ?.toEntryDownloadStatus()
            ?: EntryDownloadStatus(
                entryType = EntryType.ANIME,
                chapterId = chapterId,
                state = if (
                    animeDownloadManager.isEpisodeDownloaded(
                        episodeName = chapterName,
                        episodeUrl = chapterUrl,
                        animeTitle = entryTitle,
                        sourceId = sourceId,
                    )
                ) {
                    EntryDownloadState.DOWNLOADED
                } else {
                    EntryDownloadState.NOT_DOWNLOADED
                },
            )
    }

    override fun cancelQueuedDownload(chapterId: Long): EntryDownloadStatus? {
        val download = animeDownloadManager.queueState.value
            .firstOrNull { it.episode.id == chapterId }
            ?: return null
        animeDownloadManager.removeFromQueue(listOf(chapterId))
        download.status = AnimeDownload.State.NOT_DOWNLOADED
        return download.toEntryDownloadStatus().requireAnime()
    }

    private suspend fun downloadPreferences(entry: Entry): DownloadPreferences {
        return dependencies.downloadPreferencesRepository.getByEntryId(entry.id)
            ?: createDefaultVideoDownloadPreferences(entry.id)
    }
}

private fun entryDownloadOptions(
    context: Context,
    preferences: DownloadPreferences,
    dubOptions: List<EntryDownloadOption> = emptyList(),
    streamOptions: List<EntryDownloadOption> = emptyList(),
    subtitleOptions: List<EntryDownloadOption> = emptyList(),
): EntryDownloadOptions {
    return EntryDownloadOptions(
        groups = listOf(
            EntryDownloadOptionGroup(
                key = OPTION_QUALITY,
                label = context.stringResource(MR.strings.anime_download_quality),
                options = VideoDownloadQualityMode.entries.map { mode ->
                    EntryDownloadOption(
                        key = mode.name,
                        label = context.stringResource(
                            when (mode) {
                                VideoDownloadQualityMode.BEST -> MR.strings.anime_download_quality_best
                                VideoDownloadQualityMode.BALANCED -> MR.strings.anime_download_quality_balanced
                                VideoDownloadQualityMode.DATA_SAVING -> MR.strings.anime_download_quality_data_saving
                            },
                        ),
                    )
                },
                selectedKey = preferences.qualityMode.name,
                required = true,
            ),
            EntryDownloadOptionGroup(
                key = OPTION_DUB,
                label = context.stringResource(MR.strings.anime_playback_dub),
                options = dubOptions,
                selectedKey = preferences.dubKey,
                defaultLabel = context.stringResource(MR.strings.anime_download_automatic).takeIf {
                    dubOptions.isEmpty()
                },
                required = dubOptions.isNotEmpty(),
            ),
            EntryDownloadOptionGroup(
                key = OPTION_STREAM,
                label = context.stringResource(MR.strings.anime_playback_stream),
                options = streamOptions,
                selectedKey = preferences.streamKey,
                defaultLabel = context.stringResource(MR.strings.anime_download_automatic),
            ),
            EntryDownloadOptionGroup(
                key = OPTION_SUBTITLE,
                label = context.stringResource(MR.strings.anime_playback_subtitles),
                options = subtitleOptions,
                selectedKey = preferences.subtitleKey,
                defaultLabel = context.stringResource(MR.strings.none),
            ),
        ),
    )
}

private const val OPTION_QUALITY = "quality"
private const val OPTION_DUB = "dub"
private const val OPTION_STREAM = "stream"
private const val OPTION_SUBTITLE = "subtitle"

private fun Map<String, String?>.selectedValueOr(key: String, fallback: String?): String? {
    return if (containsKey(key)) get(key) else fallback
}

private fun EntryChapter.toSEntryChapter(): SEntryChapter = SEntryChapter.create().also {
    it.url = url
    it.name = name
    it.dateUpload = dateUpload
    it.chapterNumber = chapterNumber
}

private fun createDefaultVideoDownloadPreferences(entryId: Long): DownloadPreferences {
    return DownloadPreferences(
        entryId = entryId,
        dubKey = null,
        streamKey = null,
        subtitleKey = null,
        qualityMode = VideoDownloadQualityMode.BALANCED,
        updatedAt = 0L,
    )
}
