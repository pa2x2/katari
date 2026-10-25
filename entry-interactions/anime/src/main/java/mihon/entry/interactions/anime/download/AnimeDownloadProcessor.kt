package mihon.entry.interactions.anime.download

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import mihon.entry.interactions.anime.download.model.AnimeDownload
import mihon.entry.interactions.anime.runtime.requireAnime
import mihon.entry.interactions.anime.stream.OFF_SUBTITLE_KEY
import mihon.entry.interactions.anime.stream.automaticSubtitle
import mihon.entry.interactions.anime.stream.playableCandidates
import mihon.entry.interactions.anime.stream.savedSubtitle
import mihon.entry.interactions.anime.stream.streamChoiceKey
import mihon.entry.interactions.anime.stream.streamDisplayLabel
import mihon.entry.interactions.anime.stream.subtitlePreferenceKey
import mihon.entry.interactions.download.EntryBulkDownloadCandidateProcessor
import mihon.entry.interactions.download.EntryDownloadOption
import mihon.entry.interactions.download.EntryDownloadOptionGroup
import mihon.entry.interactions.download.EntryDownloadOptionSelection
import mihon.entry.interactions.download.EntryDownloadOptions
import mihon.entry.interactions.download.EntryDownloadOptionsProcessor
import mihon.entry.interactions.download.EntryDownloadOwnerResolver
import mihon.entry.interactions.download.EntryDownloadProcessor
import mihon.entry.interactions.download.EntryDownloadQueueGroup
import mihon.entry.interactions.download.EntryDownloadQueueItem
import mihon.entry.interactions.download.EntryDownloadState
import mihon.entry.interactions.download.EntryDownloadStatus
import mihon.entry.interactions.download.observeEntryDownloadQueue
import mihon.video.engine.spi.tracks.audioChoiceKey
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.entry.adapter.toSEntryChapter
import tachiyomi.domain.entry.model.DownloadPreferences
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.VideoDownloadQualityMode
import tachiyomi.domain.entry.repository.DownloadPreferencesRepository
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.PlaybackPreferencesRepository
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.*

internal class AnimeDownloadProcessor(
    private val animeDownloadManager: AnimeDownloadManager,
    private val animeDownloadCache: AnimeDownloadCache,
    private val downloadPreferencesRepository: DownloadPreferencesRepository,
    private val playbackPreferencesRepository: PlaybackPreferencesRepository,
    private val sourceManager: SourceManager,
    private val entryChapterRepository: EntryChapterRepository,
    entryRepository: EntryRepository,
) : EntryDownloadProcessor,
    EntryDownloadOptionsProcessor,
    EntryBulkDownloadCandidateProcessor {
    private val ownerResolver = EntryDownloadOwnerResolver(entryRepository)

    override val changes: Flow<Unit> = animeDownloadManager.cacheChanges
    override val isInitializing: Flow<Boolean> = animeDownloadCache.isInitializing
    override val isRunning: Flow<Boolean> = animeDownloadManager.isRunning
    override val queueState: Flow<List<EntryDownloadQueueGroup>> = observeEntryDownloadQueue(
        queue = animeDownloadManager.queueState,
        statusUpdates = animeDownloadManager.statusFlow(),
        progressUpdates = animeDownloadManager.progressFlow(),
    ) { downloads -> downloads.toAnimeEntryDownloadQueueGroups(sourceManager).map { it.requireAnime() } }
    override val events = animeDownloadManager.events

    override fun updates(): Flow<EntryDownloadStatus> {
        return merge(
            animeDownloadManager.statusFlow().map { it.toEntryDownloadStatus() },
            animeDownloadManager.progressFlow().map { it.toEntryDownloadStatus() },
        ).map { it.requireAnime() }
    }

    override suspend fun hasPendingDownloads(): Boolean = animeDownloadManager.hasPendingDownloads()

    override suspend fun runDownloadsUntilIdle() {
        animeDownloadManager.runDownloadsUntilIdle()
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
        animeDownloadCache.invalidateCache()
    }

    override fun renameSource(oldSource: UnifiedSource, newSource: UnifiedSource) {
        animeDownloadManager.renameSource(oldSource, newSource)
    }

    override suspend fun renameEntry(entry: Entry, newTitle: String) {
        entry.requireAnime()
        animeDownloadManager.renameAnime(entry, newTitle)
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
        queueByOwner(entry, chapters, autoStart) { owner -> downloadPreferences(owner) }
    }

    override suspend fun download(entry: Entry, chapters: List<EntryChapter>, startNow: Boolean) {
        entry.requireAnime()
        queueByOwner(entry, chapters, autoStart = false) { owner -> downloadPreferences(owner) }
        startQueuedDownloads(chapters, startNow)
    }

    override suspend fun downloadWithOptions(
        entry: Entry,
        chapters: List<EntryChapter>,
        selection: EntryDownloadOptionSelection,
        startNow: Boolean,
    ) {
        entry.requireAnime()
        queueByOwner(entry, chapters, autoStart = false) { owner ->
            downloadPreferences(owner)
                .picked(selection)
                .copy(entryId = owner.id, updatedAt = System.currentTimeMillis())
                .also { downloadPreferencesRepository.upsert(it) }
        }
        startQueuedDownloads(chapters, startNow)
    }

    override suspend fun resolveDownloadOptions(
        context: Context,
        entry: Entry,
        chapter: EntryChapter,
        selection: EntryDownloadOptionSelection?,
    ): EntryDownloadOptions {
        entry.requireAnime()
        val preferences = downloadPreferences(entry).let { saved -> selection?.let(saved::picked) ?: saved }
        val source = sourceManager.get(entry.source)
            ?: return entryDownloadOptions(context, preferences, listingFailed = true)
        val media = runCatching {
            source.getMedia(
                chapter.toSEntryChapter(),
                PlaybackSelection(
                    dubKey = preferences.dubKey,
                    serverKey = preferences.serverKey,
                    streamKey = preferences.streamKey,
                ),
            )
        }.getOrNull()
        val playback = (media as? EntryMedia.Playback)?.descriptor
            ?: return entryDownloadOptions(context, preferences, listingFailed = true)
        val streams = playableCandidates(source, playback.streams)
        // Subtitles and audio tracks belong to a stream; offer the ones of the stream a download would pick. A stream
        // that still has to be resolved shows only the tracks its source listed up front.
        val downloadStream = rankDownloadStreams(preferences, streams, playback.selection.streamKey).firstOrNull()
        val subtitles = downloadStream?.subtitles.orEmpty().filter { it.request.url.isNotBlank() }
        val audioTracks = downloadStream?.audioTracks.orEmpty()

        return entryDownloadOptions(
            context = context,
            preferences = preferences,
            dubOptions = playback.dubs.map { EntryDownloadOption(it.key, it.label) },
            serverOptions = playback.servers.map { EntryDownloadOption(it.key, it.label) },
            streamOptions = streams
                .map { stream -> EntryDownloadOption(streamChoiceKey(stream), streamDisplayLabel(stream)) }
                .distinctBy(EntryDownloadOption::key),
            subtitleOptions = subtitles
                .map { subtitle ->
                    EntryDownloadOption(
                        key = subtitlePreferenceKey(subtitle),
                        label = subtitle.language?.takeIf(String::isNotBlank)
                            ?.let { "${subtitle.label} ($it)" }
                            ?: subtitle.label,
                    )
                }
                .distinctBy(EntryDownloadOption::key),
            // Older releases saved the subtitle's choice key; the sheet shows it as the row it names.
            subtitleKey = preferences.subtitleKey
                ?.let { key -> savedSubtitle(key, subtitles)?.let(::subtitlePreferenceKey) ?: key },
            automaticSubtitleKey = automaticSubtitle(
                playbackPreferencesRepository.getByEntryId(entry.id)?.subtitleKey,
                subtitles,
            )?.let(::subtitlePreferenceKey) ?: OFF_SUBTITLE_KEY,
            audioOptions = audioTracks
                .map { track ->
                    EntryDownloadOption(
                        key = audioChoiceKey(track),
                        label = track.language?.takeIf(String::isNotBlank)
                            ?.let { "${track.label} ($it)" }
                            ?: track.label,
                    )
                }
                .distinctBy(EntryDownloadOption::key),
        )
    }

    override suspend fun resolveBulkDownloadCandidatePool(
        entry: Entry,
        candidates: List<EntryChapter>?,
    ): List<EntryChapter> {
        entry.requireAnime()
        if (candidates != null) return candidates
        val episodes = entryChapterRepository.getChaptersByEntryIdAwait(
            entry.id,
            applyScanlatorFilter = true,
        )
        val downloadedEpisodeIds = animeDownloadManager.getDownloadedEpisodeIds(episodes, entry)
        return episodes.filterNot { it.id in downloadedEpisodeIds }
    }

    override suspend fun delete(entry: Entry, chapters: List<EntryChapter>) {
        entry.requireAnime()
        animeDownloadManager.deleteEpisodes(entry, chapters)
    }

    override suspend fun deleteEntryDownloads(entry: Entry): Boolean {
        entry.requireAnime()
        return animeDownloadManager.deleteAnime(entry)
    }

    override fun hasDownloads(entry: Entry): Boolean {
        entry.requireAnime()
        return animeDownloadManager.getDownloadCount(entry) > 0
    }

    override fun getDownloadCount(entry: Entry): Int {
        entry.requireAnime()
        return animeDownloadManager.getDownloadCount(entry)
    }

    override fun getDownloadCounts(entries: List<Entry>): Map<Long, Int> {
        entries.forEach { it.requireAnime() }
        return animeDownloadManager.getDownloadCounts(entries)
    }

    override fun getTotalDownloadCount(): Int {
        return animeDownloadManager.getTotalDownloadCount()
    }

    override fun isDownloaded(entry: Entry, chapter: EntryChapter, skipCache: Boolean): Boolean {
        entry.requireAnime()
        return animeDownloadManager.isEpisodeDownloaded(
            episodeName = chapter.name,
            episodeUrl = chapter.url,
            animeTitle = entry.downloadFolderTitle,
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
        return animeDownloadManager.getQueuedDownloadsByEpisodeId()[chapterId]
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

    override fun getStatuses(entry: Entry, chapters: List<EntryChapter>): Map<Long, EntryDownloadStatus> {
        entry.requireAnime()
        val queuedDownloads = animeDownloadManager.getQueuedDownloadsByEpisodeId()
        val downloadedEpisodeIds = animeDownloadManager.getDownloadedEpisodeIds(chapters, entry)
        return chapters.associate { episode ->
            episode.id to (
                queuedDownloads[episode.id]?.toEntryDownloadStatus()
                    ?: EntryDownloadStatus(
                        entryType = EntryType.ANIME,
                        chapterId = episode.id,
                        state = if (episode.id in downloadedEpisodeIds) {
                            EntryDownloadState.DOWNLOADED
                        } else {
                            EntryDownloadState.NOT_DOWNLOADED
                        },
                    )
                )
        }
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
        return downloadPreferencesRepository.getByEntryId(entry.id)
            ?: createDefaultVideoDownloadPreferences(entry.id)
    }

    private suspend fun queueByOwner(
        entry: Entry,
        chapters: List<EntryChapter>,
        autoStart: Boolean,
        preferences: suspend (Entry) -> DownloadPreferences,
    ) {
        val owners = ownerResolver.resolve(entry, chapters)
        owners.forEach { owner ->
            animeDownloadManager.queueEpisodes(
                anime = owner.entry,
                episodes = owner.children,
                preferences = preferences(owner.entry),
                autoStart = false,
            )
        }
        if (autoStart && owners.isNotEmpty()) animeDownloadManager.startDownloads()
    }

    private fun startQueuedDownloads(chapters: List<EntryChapter>, startNow: Boolean) {
        if (startNow) {
            animeDownloadManager.startDownloadsNow(chapters.map(EntryChapter::id))
        } else {
            animeDownloadManager.startDownloads()
        }
    }
}

private fun entryDownloadOptions(
    context: Context,
    preferences: DownloadPreferences,
    dubOptions: List<EntryDownloadOption> = emptyList(),
    serverOptions: List<EntryDownloadOption> = emptyList(),
    streamOptions: List<EntryDownloadOption> = emptyList(),
    subtitleOptions: List<EntryDownloadOption> = emptyList(),
    subtitleKey: String? = preferences.subtitleKey,
    automaticSubtitleKey: String = OFF_SUBTITLE_KEY,
    audioOptions: List<EntryDownloadOption> = emptyList(),
    listingFailed: Boolean = false,
): EntryDownloadOptions {
    return EntryDownloadOptions(
        listingFailed = listingFailed,
        groups = listOfNotNull(
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
                // Servers are listed per dub.
                reloadsOptions = true,
            ),
            EntryDownloadOptionGroup(
                key = OPTION_SERVER,
                label = context.stringResource(MR.strings.anime_playback_server),
                options = serverOptions,
                selectedKey = preferences.serverKey,
                defaultLabel = context.stringResource(MR.strings.anime_download_automatic),
                // Each server has its own streams, and a stream its own subtitles and audio.
                reloadsOptions = true,
            ).takeIf { serverOptions.size > 1 },
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
                options = listOf(EntryDownloadOption(OFF_SUBTITLE_KEY, context.stringResource(MR.strings.none))) +
                    subtitleOptions,
                selectedKey = subtitleKey,
                automaticKey = automaticSubtitleKey,
            ),
            EntryDownloadOptionGroup(
                key = OPTION_AUDIO,
                label = context.stringResource(MR.strings.anime_playback_audio),
                options = audioOptions,
                selectedKey = preferences.audioKey,
                defaultLabel = context.stringResource(MR.strings.anime_download_automatic),
            ).takeIf { audioOptions.size > 1 },
        ),
    )
}

private const val OPTION_QUALITY = "quality"
private const val OPTION_DUB = "dub"
private const val OPTION_SERVER = "server"
private const val OPTION_STREAM = "stream"
private const val OPTION_SUBTITLE = "subtitle"
private const val OPTION_AUDIO = "audio"

/** These preferences with the sheet's picks in place; a group the sheet didn't offer keeps its saved choice. */
private fun DownloadPreferences.picked(selection: EntryDownloadOptionSelection): DownloadPreferences {
    val values = selection.values
    return copy(
        dubKey = values.selectedValueOr(OPTION_DUB, dubKey),
        serverKey = values.selectedValueOr(OPTION_SERVER, serverKey),
        streamKey = values.selectedValueOr(OPTION_STREAM, streamKey),
        subtitleKey = values.selectedValueOr(OPTION_SUBTITLE, subtitleKey),
        audioKey = values.selectedValueOr(OPTION_AUDIO, audioKey),
        qualityMode = values[OPTION_QUALITY]
            ?.let { runCatching { VideoDownloadQualityMode.valueOf(it) }.getOrNull() }
            ?: qualityMode,
    )
}

private fun Map<String, String?>.selectedValueOr(key: String, fallback: String?): String? {
    return if (containsKey(key)) get(key) else fallback
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
