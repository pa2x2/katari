package eu.kanade.tachiyomi.ui.video.player

import android.app.Application
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.PlaybackDescriptor
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.entry.VideoRequest
import eu.kanade.tachiyomi.source.entry.VideoStream
import eu.kanade.tachiyomi.source.entry.VideoStreamType
import eu.kanade.tachiyomi.source.entry.VideoSubtitle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import logcat.LogPriority
import mihon.entry.interactions.anime.download.AnimeDownloadProvider
import mihon.entry.interactions.anime.download.model.AnimeDownloadManifest
import mihon.entry.interactions.anime.stream.PreferredHeight
import mihon.entry.interactions.anime.stream.chooseStream
import mihon.entry.interactions.anime.stream.pickPreferredSourceQuality
import mihon.entry.interactions.anime.stream.streamChoiceKey
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.adapter.toSEntryChapter
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.PlaybackPreferences
import tachiyomi.domain.entry.model.PlayerQualityMode
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.PlaybackPreferencesRepository
import tachiyomi.domain.source.service.SourceManager
import kotlin.time.Duration.Companion.milliseconds

internal class ResolveVideoStream(
    private val videoRepository: EntryRepository,
    private val entryChapterRepository: EntryChapterRepository,
    private val playbackPreferencesRepository: PlaybackPreferencesRepository,
    private val videoSourceManager: SourceManager,
    private val animeDownloadProvider: AnimeDownloadProvider,
    private val context: Application,
    private val isOnline: () -> Boolean = context::isOnline,
    private val sourceInitTimeoutMs: Long = SOURCE_INIT_TIMEOUT_MS,
    private val streamFetchTimeoutMs: Long = STREAM_FETCH_TIMEOUT_MS,
    /** The height to start a series at before one is picked in the player; null leaves it to the source. */
    private val preferredHeight: () -> PreferredHeight? = { null },
) : VideoStreamResolver {

    override suspend operator fun invoke(
        entryId: Long,
        chapterId: Long,
        ownerEntryId: Long,
        selection: PlaybackSelection?,
    ): Result {
        val visibleEntry = runCatchingCancellable { videoRepository.getEntryById(entryId) }
            .getOrElse { return Result.Error(Reason.VideoNotFound) }
            ?: return Result.Error(Reason.VideoNotFound)
        val ownerEntry = runCatchingCancellable { videoRepository.getEntryById(ownerEntryId) }
            .getOrElse { return Result.Error(Reason.VideoNotFound) }
            ?: return Result.Error(Reason.VideoNotFound)
        val chapter = entryChapterRepository.getChapterById(chapterId)
            ?: return Result.Error(Reason.EpisodeNotFound)

        if (chapter.entryId != ownerEntry.id) {
            return Result.Error(Reason.EpisodeMismatch)
        }

        val savedPreferences = playbackPreferencesRepository.getByEntryId(ownerEntry.id)
            ?: defaultPlaybackPreferences(ownerEntry.id)

        tryDownloadedPlayback(visibleEntry, ownerEntry, chapter, savedPreferences)?.let { return it }

        val initialized = withTimeoutOrNull(sourceInitTimeoutMs.milliseconds) {
            videoSourceManager.isInitialized.first { it }
        } == true

        if (initialized) {
            tryDownloadedPlayback(visibleEntry, ownerEntry, chapter, savedPreferences)?.let { return it }
        }

        // No downloaded episode found — if offline, fail with a clear message.
        if (!isOnline()) {
            return Result.Error(Reason.OfflineNoDownload)
        }

        // Online path: wait for full source initialization before network requests.
        if (!initialized) {
            return Result.Error(Reason.SourceLoadTimeout)
        }

        val source = videoSourceManager.get(ownerEntry.source)
            ?: return Result.Error(Reason.SourceNotFound)

        val requestedSelection = selection ?: PlaybackSelection(
            dubKey = savedPreferences.dubKey,
            streamKey = savedPreferences.streamKey,
            sourceQualityKey = savedPreferences.sourceQualityKey,
        )
        val sourceEpisode = chapter.toSEntryChapter()

        logcat(LogPriority.DEBUG) {
            "ResolveVideoStream: start entryId=$entryId episodeId=$chapterId source=${ownerEntry.source}"
        }
        val media = runCatchingCancellable {
            withTimeoutOrNull(streamFetchTimeoutMs.milliseconds) {
                source.getMedia(sourceEpisode, requestedSelection)
            } ?: return Result.Error(Reason.StreamFetchTimeout)
        }.getOrElse {
            logcat(LogPriority.DEBUG, it) {
                "ResolveVideoStream: source.getMedia threw for entryId=$entryId episodeId=$chapterId"
            }
            return Result.Error(Reason.StreamFetchFailed(it))
        }

        val firstPlaybackData = (media as? EntryMedia.Playback)?.descriptor
            ?: return Result.Error(Reason.NoStreams)
        val playbackData = if (selection == null && savedPreferences.sourceQualityKey == null) {
            withPreferredSourceQuality(source, sourceEpisode, requestedSelection, firstPlaybackData)
        } else {
            firstPlaybackData
        }
        logcat(LogPriority.DEBUG) {
            "ResolveVideoStream: source.getMedia returned ${playbackData.streams.size} streams for entryId=$entryId episodeId=$chapterId"
        }

        val validStreams = playbackData.streams.filter { it.request?.url?.isNotBlank() == true }
        val chosenStream = chooseStream(
            streams = validStreams,
            savedKey = requestedSelection.streamKey,
            sourceKey = playbackData.selection.streamKey,
            preferredHeight = preferredHeight(),
        ) ?: return Result.Error(Reason.NoStreams)
        val stream = chosenStream.copy(subtitles = chosenStream.subtitles.filter { it.request.url.isNotBlank() })

        val resolvedPlaybackData = playbackData.copy(
            selection = playbackData.selection.copy(
                streamKey = streamChoiceKey(stream),
            ),
        )
        val resolvedSelection = resolvedPlaybackData.selection
        val selectionChanged =
            savedPreferences.dubKey != resolvedSelection.dubKey ||
                savedPreferences.streamKey != resolvedSelection.streamKey ||
                savedPreferences.sourceQualityKey != resolvedSelection.sourceQualityKey
        val resolvedPreferences = if (selectionChanged) {
            savedPreferences.copy(
                dubKey = resolvedSelection.dubKey,
                streamKey = resolvedSelection.streamKey,
                sourceQualityKey = resolvedSelection.sourceQualityKey,
                updatedAt = System.currentTimeMillis(),
            )
        } else {
            savedPreferences
        }
        if (selectionChanged) {
            playbackPreferencesRepository.upsert(resolvedPreferences)
        }

        return Result.Success(
            visibleEntry = visibleEntry,
            ownerEntry = ownerEntry,
            chapter = chapter,
            playbackData = resolvedPlaybackData,
            stream = stream,
            savedPreferences = resolvedPreferences,
        )
    }

    /**
     * A series' first episode, before any quality was saved for it: sources then pick on their own, often the lowest,
     * so ask again for the preferred one. The quality resolved here is saved below and used from then on. Any failure
     * keeps the first answer.
     */
    private suspend fun withPreferredSourceQuality(
        source: UnifiedSource,
        sourceEpisode: SEntryChapter,
        requestedSelection: PlaybackSelection,
        playbackData: PlaybackDescriptor,
    ): PlaybackDescriptor {
        val preferredHeight = preferredHeight() ?: return playbackData
        val preferredKey = pickPreferredSourceQuality(playbackData.sourceQualities, preferredHeight)
        if (preferredKey == null || preferredKey == playbackData.selection.sourceQualityKey) return playbackData
        val media = runCatchingCancellable {
            withTimeoutOrNull(streamFetchTimeoutMs.milliseconds) {
                source.getMedia(sourceEpisode, requestedSelection.copy(sourceQualityKey = preferredKey))
            }
        }.getOrNull()
        return (media as? EntryMedia.Playback)?.descriptor?.takeIf { it.streams.isNotEmpty() } ?: playbackData
    }

    sealed interface Result {
        data class Success(
            val visibleEntry: Entry,
            val ownerEntry: Entry,
            val chapter: EntryChapter,
            val playbackData: PlaybackDescriptor,
            val stream: VideoStream,
            val savedPreferences: PlaybackPreferences,
        ) : Result

        data class Error(val reason: Reason) : Result
    }

    sealed interface Reason {
        data object VideoNotFound : Reason

        data object EpisodeNotFound : Reason

        data object EpisodeMismatch : Reason

        data object SourceLoadTimeout : Reason

        data object SourceNotFound : Reason

        data object NoStreams : Reason

        data object StreamFetchTimeout : Reason

        data object OfflineNoDownload : Reason

        data class StreamFetchFailed(val cause: Throwable) : Reason
    }

    private fun tryDownloadedPlayback(
        visibleEntry: Entry,
        ownerEntry: Entry,
        chapter: EntryChapter,
        savedPreferences: PlaybackPreferences,
    ): Result.Success? {
        val sourceCandidates = listOf(visibleEntry.source, ownerEntry.source)
            .distinct()
            .mapNotNull(videoSourceManager::get)
        val entryCandidates = listOf(visibleEntry, ownerEntry).distinctBy(Entry::id)

        for (entry in entryCandidates) {
            for (source in sourceCandidates) {
                val episodeDir = animeDownloadProvider.findEpisodeDir(
                    episodeName = chapter.name,
                    episodeUrl = chapter.url,
                    animeTitle = entry.downloadFolderTitle,
                    source = source,
                ) ?: continue
                val downloaded = readDownloadedPlayback(episodeDir) ?: continue

                return Result.Success(
                    visibleEntry = visibleEntry,
                    ownerEntry = ownerEntry,
                    chapter = chapter,
                    playbackData = downloaded.playbackData,
                    stream = downloaded.stream,
                    savedPreferences = savedPreferences,
                )
            }
        }
        return null
    }

    private fun readDownloadedPlayback(episodeDir: UniFile): DownloadedPlaybackData? {
        val manifest = animeDownloadProvider.readValidManifest(episodeDir)

        if (manifest != null) {
            val videoFile = resolveDownloadedVideoFile(episodeDir, manifest) ?: return null
            val subtitles = manifest.subtitles.mapNotNull { subtitle ->
                val subtitleFile = episodeDir.findFile(subtitle.fileName) ?: return@mapNotNull null
                VideoSubtitle(
                    request = VideoRequest(subtitleFile.uri.toString()),
                    label = subtitle.label,
                    language = subtitle.language,
                    mimeType = subtitle.mimeType,
                    key = subtitle.key,
                    isDefault = subtitle.isDefault,
                    isForced = subtitle.isForced,
                )
            }
            val stream = VideoStream(
                request = VideoRequest(videoFile.uri.toString()),
                label = manifest.video.label,
                type = manifest.video.streamType,
                mimeType = manifest.video.mimeType,
                key = manifest.selection.streamKey.orEmpty(),
                subtitles = subtitles,
            )
            return DownloadedPlaybackData(
                playbackData = PlaybackDescriptor(
                    selection = manifest.selection,
                    streams = listOf(stream),
                ),
                stream = stream,
            )
        }

        val videoFile = episodeDir.listFiles()?.firstOrNull { file ->
            val name = file.name.orEmpty()
            LEGACY_VIDEO_EXTENSIONS.any { name.endsWith(it, ignoreCase = true) }
        } ?: return null
        val isHls = videoFile.name?.endsWith(".m3u8", ignoreCase = true) == true ||
            videoFile.name?.endsWith(".m3u", ignoreCase = true) == true
        val stream = VideoStream(
            request = VideoRequest(videoFile.uri.toString()),
            label = "Downloaded",
            type = if (isHls) VideoStreamType.HLS else VideoStreamType.PROGRESSIVE,
            key = DOWNLOADED_STREAM_KEY,
        )
        return DownloadedPlaybackData(
            playbackData = PlaybackDescriptor(
                selection = PlaybackSelection(streamKey = DOWNLOADED_STREAM_KEY),
                streams = listOf(stream),
            ),
            stream = stream,
        )
    }

    private fun resolveDownloadedVideoFile(
        episodeDir: UniFile,
        manifest: AnimeDownloadManifest,
    ): UniFile? {
        if (manifest.video.streamType != VideoStreamType.HLS) {
            return episodeDir.findFile(manifest.video.fileName)
        }

        val preferredNames = buildList {
            add(DOWNLOADED_HLS_ROOT_PLAYLIST)
            add(DOWNLOADED_HLS_LEGACY_ROOT_PLAYLIST)
            add(manifest.video.fileName)
            manifest.video.fileName.substringBeforeLast('.').takeIf(String::isNotBlank)?.let { baseName ->
                add("$baseName.m3u8")
                add("$baseName.m3u")
            }
        }.distinct()
        preferredNames.firstNotNullOfOrNull(episodeDir::findFile)?.let { return it }

        return episodeDir.listFiles()?.firstOrNull { file ->
            file.name?.endsWith(".m3u8", ignoreCase = true) == true ||
                file.name?.endsWith(".m3u", ignoreCase = true) == true
        }
    }

    private fun defaultPlaybackPreferences(entryId: Long): PlaybackPreferences {
        return PlaybackPreferences(
            entryId = entryId,
            dubKey = null,
            streamKey = null,
            sourceQualityKey = null,
            subtitleKey = null,
            playerQualityMode = PlayerQualityMode.AUTO,
            playerQualityHeight = null,
            subtitleOffsetX = null,
            subtitleOffsetY = null,
            subtitleTextSize = null,
            subtitleTextColor = null,
            subtitleBackgroundColor = null,
            subtitleBackgroundOpacity = null,
            updatedAt = 0L,
        )
    }

    private companion object {
        private const val DOWNLOADED_HLS_ROOT_PLAYLIST = "video.m3u8"
        private const val DOWNLOADED_HLS_LEGACY_ROOT_PLAYLIST = "video.m3u"
        private const val DOWNLOADED_STREAM_KEY = "downloaded"
        private val LEGACY_VIDEO_EXTENSIONS = listOf(".mp4", ".mkv", ".m3u8", ".m3u")
        private const val SOURCE_INIT_TIMEOUT_MS = 5_000L
        private const val STREAM_FETCH_TIMEOUT_MS = 15_000L
    }
}

private data class DownloadedPlaybackData(
    val playbackData: PlaybackDescriptor,
    val stream: VideoStream,
)

private inline fun <T> runCatchingCancellable(block: () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }
}
