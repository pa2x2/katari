package mihon.entry.interactions.anime.download

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.VideoAudioTrack
import eu.kanade.tachiyomi.source.entry.VideoRequest
import eu.kanade.tachiyomi.source.entry.VideoStream
import eu.kanade.tachiyomi.source.entry.VideoStreamType
import eu.kanade.tachiyomi.source.entry.VideoSubtitle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import logcat.LogPriority
import mihon.entry.interactions.anime.download.model.AnimeDownload
import mihon.entry.interactions.anime.download.model.AnimeDownloadFailure
import mihon.entry.interactions.anime.download.model.AnimeDownloadManifest
import mihon.entry.interactions.anime.download.model.DownloadedAudioTrack
import mihon.entry.interactions.anime.download.model.DownloadedSubtitle
import mihon.entry.interactions.anime.download.model.DownloadedVideo
import mihon.entry.interactions.anime.download.stream.DashDownload
import mihon.entry.interactions.anime.download.stream.HlsDownload
import mihon.entry.interactions.anime.download.stream.UndownloadableStreamException
import mihon.entry.interactions.anime.download.stream.VideoDownloadProgressTracker
import mihon.entry.interactions.anime.download.stream.WholeFileTransfer
import mihon.entry.interactions.anime.download.stream.createFileTransferClient
import mihon.entry.interactions.anime.download.stream.mediaFileExtension
import mihon.entry.interactions.anime.stream.OFF_SUBTITLE_KEY
import mihon.entry.interactions.anime.stream.PreferredHeight
import mihon.entry.interactions.anime.stream.StreamTypeProbe
import mihon.entry.interactions.anime.stream.TorrentStreamException
import mihon.entry.interactions.anime.stream.automaticSubtitle
import mihon.entry.interactions.anime.stream.declaredStreamType
import mihon.entry.interactions.anime.stream.firstPlayableStream
import mihon.entry.interactions.anime.stream.isTorrent
import mihon.entry.interactions.anime.stream.pickPreferredSourceQuality
import mihon.entry.interactions.anime.stream.playableCandidates
import mihon.entry.interactions.anime.stream.rankStreams
import mihon.entry.interactions.anime.stream.resolveDeferredStream
import mihon.entry.interactions.anime.stream.savedSubtitle
import mihon.entry.interactions.anime.stream.streamChoiceKey
import mihon.entry.interactions.anime.stream.streamDisplayLabel
import mihon.video.engine.spi.loading.resolvedRequest
import mihon.video.engine.spi.tracks.audioChoiceKey
import mihon.video.engine.spi.tracks.subtitleChoiceKey
import tachiyomi.core.common.preference.SourcePreferenceProfile
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.adapter.toSEntryChapter
import tachiyomi.domain.entry.model.DownloadPreferences
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.VideoDownloadQualityMode
import tachiyomi.domain.entry.repository.PlaybackPreferencesRepository
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.source.service.awaitInitialized
import java.io.IOException

internal class AnimeDownloader(
    context: Context,
    private val provider: AnimeDownloadProvider,
    private val cache: AnimeDownloadCache,
    private val sourceManager: SourceManager,
    private val playbackPreferencesRepository: PlaybackPreferencesRepository,
    private val networkHelper: NetworkHelper,
    private val streamTypeProbe: StreamTypeProbe,
    private val json: Json,
) {

    private val fileTransferClient = createFileTransferClient(networkHelper.client)
    private val hlsDownload = HlsDownload(
        playlistClient = fileTransferClient,
        assetClient = networkHelper.client,
    )
    private val dashDownload = DashDownload(fileTransferClient)
    private val wholeFileTransfer = WholeFileTransfer(context.contentResolver, fileTransferClient)

    // Extensions read logins and API keys from the entry's profile's source preferences, not the active profile's.
    suspend fun download(download: AnimeDownload): AnimeDownloadFailure? =
        withContext(SourcePreferenceProfile.of(download.anime.profileId)) { downloadInProfile(download) }

    private suspend fun downloadInProfile(download: AnimeDownload): AnimeDownloadFailure? {
        // The restored queue resumes as the process starts, before the sources have loaded
        sourceManager.awaitInitialized()
        val source = sourceManager.get(download.anime.source)
            ?: return AnimeDownloadFailure(AnimeDownloadFailure.Reason.SOURCE_NOT_FOUND)

        val selectedDubKey = download.preferences.dubKey
        val requestedSelection = PlaybackSelection(
            dubKey = selectedDubKey,
            serverKey = download.preferences.serverKey,
            streamKey = download.preferences.streamKey,
        )

        var playbackData = runCatching {
            val media = source.getMedia(download.episode.toSEntryChapter(), requestedSelection)
            (media as? EntryMedia.Playback)?.descriptor
        }.getOrElse {
            return AnimeDownloadFailure(
                reason = AnimeDownloadFailure.Reason.NETWORK,
                message = it.message,
            )
        } ?: return AnimeDownloadFailure(AnimeDownloadFailure.Reason.STREAM_NOT_AVAILABLE)

        if (download.preferences.streamKey == null) {
            val selectedSourceQualityKey = pickPreferredSourceQuality(
                options = playbackData.sourceQualities,
                preferred = download.preferences.qualityMode.preferredHeight,
            )
            if (
                selectedSourceQualityKey != null &&
                selectedSourceQualityKey != playbackData.selection.sourceQualityKey
            ) {
                playbackData = runCatching {
                    val media = source.getMedia(
                        download.episode.toSEntryChapter(),
                        playbackData.selection.copy(sourceQualityKey = selectedSourceQualityKey),
                    )
                    (media as? EntryMedia.Playback)?.descriptor
                }.getOrElse {
                    return AnimeDownloadFailure(
                        reason = AnimeDownloadFailure.Reason.NETWORK,
                        message = it.message,
                    )
                } ?: return AnimeDownloadFailure(AnimeDownloadFailure.Reason.STREAM_NOT_AVAILABLE)
            }
        }

        if (selectedDubKey != null && playbackData.selection.dubKey != selectedDubKey) {
            return AnimeDownloadFailure(AnimeDownloadFailure.Reason.DUB_NOT_AVAILABLE)
        }

        val rankedStreams = rankDownloadStreams(
            preferences = download.preferences,
            streams = playableCandidates(source, playbackData.streams),
            sourceKey = playbackData.selection.streamKey,
        )
        if (rankedStreams.isEmpty()) {
            val reason = if (playbackData.streams.any { it.isTorrent }) {
                AnimeDownloadFailure.Reason.TORRENT_ONLY
            } else {
                AnimeDownloadFailure.Reason.STREAM_NOT_AVAILABLE
            }
            return AnimeDownloadFailure(reason)
        }
        val sourceEpisode = download.episode.toSEntryChapter()
        // Resolved just before downloading, since resolved links expire.
        val stream = firstPlayableStream(rankedStreams, explicitPick = download.preferences.streamKey != null) {
            streamTypeProbe.typed(if (it.request == null) resolveDeferredStream(source, sourceEpisode, it) else it)
        }.getOrElse {
            if (it is TorrentStreamException) {
                return AnimeDownloadFailure(AnimeDownloadFailure.Reason.TORRENT_ONLY)
            }
            return AnimeDownloadFailure(reason = AnimeDownloadFailure.Reason.NETWORK, message = it.message)
        }
        if (stream.type !in DOWNLOADABLE_STREAM_TYPES) {
            return AnimeDownloadFailure(AnimeDownloadFailure.Reason.UNSUPPORTED_STREAM)
        }

        val subtitleCandidates = stream.subtitles.filter { it.request.url.isNotBlank() }
        val subtitles = when (val selectedKey = download.preferences.subtitleKey) {
            null -> automaticSubtitle(
                playbackPreferencesRepository.getByEntryId(download.anime.id)?.subtitleKey,
                subtitleCandidates,
            )
            OFF_SUBTITLE_KEY -> null
            else -> savedSubtitle(selectedKey, subtitleCandidates)
                ?: return AnimeDownloadFailure(AnimeDownloadFailure.Reason.SUBTITLE_NOT_AVAILABLE)
        }.let(::listOfNotNull)
        val audioTrack = selectDownloadAudioTrack(stream.audioTracks, download.preferences.audioKey)

        val animeDir = provider.getAnimeDir(download.anime.downloadFolderTitle, source)
            .getOrElse {
                return AnimeDownloadFailure(
                    reason = AnimeDownloadFailure.Reason.UNKNOWN,
                    message = it.message,
                )
            }
        val episodeDirName = provider.getEpisodeDirName(download.episode.name, download.episode.url)
        val tmpDir = provider.beginEpisodePackage(animeDir, episodeDirName)
            .getOrElse { return AnimeDownloadFailure(AnimeDownloadFailure.Reason.UNKNOWN, it.message) }
        var packagePublished = false

        try {
            download.status = AnimeDownload.State.RESOLVING
            download.selection = playbackData.selection.copy(streamKey = streamChoiceKey(stream))
            download.playbackData = playbackData.copy(selection = download.selection)
            download.stream = stream
            download.subtitles = subtitles

            download.status = AnimeDownload.State.DOWNLOADING
            download.progress = 0

            val videoFileName = downloadVideo(download, stream, tmpDir)
            download.progress = maxOf(download.progress, if (subtitles.isEmpty()) 90 else 80)

            val audioFiles = listOfNotNull(audioTrack?.let { downloadAudioTrack(it, tmpDir) })

            val subtitleFiles = subtitles.mapIndexed { index, subtitle ->
                downloadSubtitle(index, subtitle, tmpDir)
            }
            download.progress = 95

            writeManifest(download, stream, videoFileName, subtitleFiles, audioFiles, tmpDir)

            provider.publishEpisodePackage(animeDir, tmpDir, episodeDirName).getOrThrow()
            packagePublished = true
            cache.addEpisode(episodeDirName, animeDir, download.anime)
            download.progress = 100
            download.status = AnimeDownload.State.DOWNLOADED
            return null
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            if (!packagePublished) tmpDir.delete()
            logcat(LogPriority.ERROR, e) { "Failed to download ${download.anime.title} - ${download.episode.name}" }
            return AnimeDownloadFailure(
                reason = when {
                    e is UndownloadableStreamException -> e.failureReason
                    e is UnsupportedOperationException -> AnimeDownloadFailure.Reason.UNSUPPORTED_STREAM
                    e is IOException -> AnimeDownloadFailure.Reason.NETWORK
                    else -> AnimeDownloadFailure.Reason.UNKNOWN
                },
                message = e.message ?: e::class.simpleName,
            )
        }
    }

    private suspend fun downloadVideo(download: AnimeDownload, stream: VideoStream, tmpDir: UniFile): String {
        val progressTracker = VideoDownloadProgressTracker(download)
        return when (stream.type) {
            VideoStreamType.PROGRESSIVE -> {
                val extension = mediaFileExtension(stream.resolvedRequest.url, stream.mimeType, fallback = "mp4")
                val fileName = "$VIDEO_FILE_NAME.$extension"
                val file = tmpDir.createFile(fileName) ?: error("Failed to create video file")
                wholeFileTransfer.copyInto(stream.resolvedRequest, file::openOutputStream, progressTracker)
                progressTracker.onVideoTransferComplete()
                fileName
            }
            VideoStreamType.HLS -> {
                hlsDownload.download(stream.resolvedRequest, VIDEO_FILE_NAME, tmpDir, progressTracker)
            }
            VideoStreamType.DASH -> {
                val preferredHeight = download.preferences.qualityMode.preferredHeight
                dashDownload.download(stream.resolvedRequest, preferredHeight, tmpDir, progressTracker)
            }
            else -> {
                throw UnsupportedOperationException("Unsupported stream type: ${stream.type}")
            }
        }
    }

    private suspend fun downloadAudioTrack(track: VideoAudioTrack, tmpDir: UniFile): DownloadedAudioTrack {
        if (track.type == VideoStreamType.DASH) throw UnsupportedOperationException("DASH audio track '${track.key}'")
        val isHls = track.type == VideoStreamType.HLS
        val fileName = if (isHls) {
            hlsDownload.download(track.request, AUDIO_FILE_NAME, tmpDir, progressTracker = null)
        } else {
            val name = "$AUDIO_FILE_NAME.${mediaFileExtension(track.request.url, track.mimeType, fallback = "m4a")}"
            val file = tmpDir.createFile(name) ?: error("Failed to create audio file")
            wholeFileTransfer.copyInto(track.request, file::openOutputStream)
            file.name ?: name
        }
        return DownloadedAudioTrack(
            key = audioChoiceKey(track),
            label = track.label,
            language = track.language,
            mimeType = track.mimeType,
            streamType = if (isHls) VideoStreamType.HLS else VideoStreamType.PROGRESSIVE,
            fileName = fileName,
            isDefault = track.isDefault,
        )
    }

    private suspend fun downloadSubtitle(index: Int, subtitle: VideoSubtitle, tmpDir: UniFile): DownloadedSubtitle {
        val extension = mediaFileExtension(subtitle.request.url, subtitle.mimeType, fallback = "vtt")
        val fileName = "subtitle_${index + 1}.$extension"
        val file = tmpDir.createFile(fileName) ?: error("Failed to create subtitle file")
        wholeFileTransfer.copyInto(subtitle.request, file::openOutputStream)
        return DownloadedSubtitle(
            key = subtitleChoiceKey(subtitle),
            label = subtitle.label,
            language = subtitle.language,
            mimeType = subtitle.mimeType,
            fileName = fileName,
            isDefault = subtitle.isDefault,
            isForced = subtitle.isForced,
        )
    }

    private fun writeManifest(
        download: AnimeDownload,
        stream: VideoStream,
        videoFileName: String,
        subtitleFiles: List<DownloadedSubtitle>,
        audioFiles: List<DownloadedAudioTrack>,
        tmpDir: UniFile,
    ) {
        val file = tmpDir.createFile(MANIFEST_FILE_NAME) ?: error("Failed to create anime manifest")
        val manifest = AnimeDownloadManifest(
            animeId = download.anime.id,
            episodeId = download.episode.id,
            animeTitle = download.anime.title,
            episodeTitle = download.episode.name,
            originalEpisodeUrl = download.episode.url,
            qualityMode = download.preferences.qualityMode.name,
            selection = download.selection,
            video = DownloadedVideo(
                fileName = videoFileName,
                sourceUrl = stream.resolvedRequest.url,
                headers = stream.resolvedRequest.headers,
                label = streamDisplayLabel(stream),
                streamType = stream.type,
                mimeType = stream.mimeType,
                mpvOptions = stream.mpvOptions,
            ),
            subtitles = subtitleFiles,
            audioTracks = audioFiles,
            markers = stream.markers,
            artifacts = provider.createArtifactRecords(tmpDir),
        )
        file.openOutputStream().use {
            it.write(json.encodeToString(manifest).toByteArray())
        }
    }

    companion object {
        const val MANIFEST_FILE_NAME = "anime_download.json"
        private const val VIDEO_FILE_NAME = "video"

        // Numbered like the subtitle files, though a download keeps one audio track for now.
        private const val AUDIO_FILE_NAME = "audio_1"
        private val DOWNLOADABLE_STREAM_TYPES =
            setOf(VideoStreamType.PROGRESSIVE, VideoStreamType.HLS, VideoStreamType.DASH)
    }
}

private val UndownloadableStreamException.failureReason: AnimeDownloadFailure.Reason
    get() = when (reason) {
        UndownloadableStreamException.Reason.LIVE -> AnimeDownloadFailure.Reason.LIVE_STREAM
        UndownloadableStreamException.Reason.COPY_PROTECTED -> AnimeDownloadFailure.Reason.COPY_PROTECTED
    }

/**
 * The external audio track a download keeps: the one picked in the download options, else the one the source marks
 * as default, else the first. A stream that has external audio may have no audio of its own, so it keeps one.
 */
internal fun selectDownloadAudioTrack(tracks: List<VideoAudioTrack>, audioKey: String?): VideoAudioTrack? {
    return tracks.firstOrNull { audioChoiceKey(it) == audioKey }
        ?: tracks.firstOrNull(VideoAudioTrack::isDefault)
        ?: tracks.firstOrNull()
}

/**
 * The streams a download tries, in order. A stream picked in the download options is the only one; otherwise
 * progressive files come first, since they need no playlist rewriting, and the rest follows the player's order.
 */
internal fun rankDownloadStreams(
    preferences: DownloadPreferences,
    streams: List<VideoStream>,
    sourceKey: String?,
): List<VideoStream> {
    preferences.streamKey?.let { key -> return listOfNotNull(streams.firstOrNull { streamChoiceKey(it) == key }) }
    val progressiveStreams = streams.filter { declaredStreamType(it) == VideoStreamType.PROGRESSIVE }
    return rankStreams(
        streams = progressiveStreams.ifEmpty { streams },
        savedKey = null,
        sourceKey = sourceKey,
        preferredHeight = preferences.qualityMode.preferredHeight,
    )
}

internal val VideoDownloadQualityMode.preferredHeight: PreferredHeight
    get() = when (this) {
        VideoDownloadQualityMode.BEST -> PreferredHeight.Highest
        VideoDownloadQualityMode.DATA_SAVING -> PreferredHeight.Lowest
        VideoDownloadQualityMode.BALANCED -> PreferredHeight.Near(720)
    }
