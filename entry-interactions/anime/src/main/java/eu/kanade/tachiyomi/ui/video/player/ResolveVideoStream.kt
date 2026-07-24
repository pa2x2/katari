package eu.kanade.tachiyomi.ui.video.player

import android.app.Application
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.PlaybackDescriptor
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import eu.kanade.tachiyomi.source.entry.SubtitleSource
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
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.PlaybackPreferences
import tachiyomi.domain.entry.model.PlayerQualityMode
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.PlaybackPreferencesRepository
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.time.Duration.Companion.milliseconds

internal class ResolveVideoStream(
    private val videoRepository: EntryRepository = Injekt.get(),
    private val entryChapterRepository: EntryChapterRepository = Injekt.get(),
    private val playbackPreferencesRepository: PlaybackPreferencesRepository = Injekt.get(),
    private val videoSourceManager: SourceManager = Injekt.get(),
    private val animeDownloadProvider: AnimeDownloadProvider = Injekt.get(),
    private val context: Application = Injekt.get(),
    private val isOnline: () -> Boolean = context::isOnline,
    private val sourceInitTimeoutMs: Long = SOURCE_INIT_TIMEOUT_MS,
    private val streamFetchTimeoutMs: Long = STREAM_FETCH_TIMEOUT_MS,
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

        val playbackData = (media as? EntryMedia.Playback)?.descriptor
            ?: return Result.Error(Reason.NoStreams)
        logcat(LogPriority.DEBUG) {
            "ResolveVideoStream: source.getMedia returned ${playbackData.streams.size} streams for entryId=$entryId episodeId=$chapterId"
        }

        val subtitles = if (source is SubtitleSource) {
            runCatchingCancellable {
                source.getSubtitles(sourceEpisode, playbackData.selection)
                    .filter { it.request.url.isNotBlank() }
            }.getOrElse {
                logcat(LogPriority.DEBUG, it) {
                    "ResolveVideoStream: source.getSubtitles threw for entryId=$entryId episodeId=$chapterId"
                }
                emptyList()
            }
        } else {
            emptyList()
        }

        val validStreams = playbackData.streams.filter { it.request.url.isNotBlank() }
        val stream = validStreams
            .firstOrNull { streamChoiceKey(it) == requestedSelection.streamKey }
            ?: validStreams.maxByOrNull(::streamScore)
            ?: return Result.Error(Reason.NoStreams)

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
            subtitles = subtitles,
            savedPreferences = resolvedPreferences,
        )
    }

    sealed interface Result {
        data class Success(
            val visibleEntry: Entry,
            val ownerEntry: Entry,
            val chapter: EntryChapter,
            val playbackData: PlaybackDescriptor,
            val stream: VideoStream,
            val subtitles: List<VideoSubtitle>,
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

    private fun streamScore(stream: VideoStream): Int {
        val typeScore = when (stream.type) {
            VideoStreamType.HLS -> 400
            VideoStreamType.PROGRESSIVE -> 300
            VideoStreamType.DASH -> 200
            VideoStreamType.UNKNOWN -> 100
        }
        val mimeScore = when {
            stream.mimeType?.contains("mp4", ignoreCase = true) == true -> 30
            stream.mimeType?.contains("mpegurl", ignoreCase = true) == true -> 20
            stream.mimeType?.contains("dash", ignoreCase = true) == true -> 10
            else -> 0
        }
        val headerScore = if (stream.request.headers.isNotEmpty()) 5 else 0
        val labelScore = if (stream.label.isNotBlank()) 1 else 0
        return typeScore + mimeScore + headerScore + labelScore
    }

    private fun streamChoiceKey(stream: VideoStream): String {
        return stream.key.ifBlank {
            stream.label.ifBlank { stream.request.url }
        }
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
                    animeTitle = entry.title,
                    source = source,
                ) ?: continue
                val downloaded = readDownloadedPlayback(episodeDir) ?: continue

                return Result.Success(
                    visibleEntry = visibleEntry,
                    ownerEntry = ownerEntry,
                    chapter = chapter,
                    playbackData = downloaded.playbackData,
                    stream = downloaded.stream,
                    subtitles = downloaded.subtitles,
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
            val stream = VideoStream(
                request = VideoRequest(videoFile.uri.toString()),
                label = manifest.video.label,
                type = manifest.video.streamType,
                mimeType = manifest.video.mimeType,
                key = manifest.selection.streamKey.orEmpty(),
            )
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
            return DownloadedPlaybackData(
                playbackData = PlaybackDescriptor(
                    selection = manifest.selection,
                    streams = listOf(stream),
                ),
                stream = stream,
                subtitles = subtitles,
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
            subtitles = emptyList(),
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
    val subtitles: List<VideoSubtitle>,
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

private fun EntryChapter.toSEntryChapter(): SEntryChapter = SEntryChapter.create().also {
    it.url = url
    it.name = name
    it.dateUpload = dateUpload
    it.chapterNumber = chapterNumber
}
