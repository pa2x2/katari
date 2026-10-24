package mihon.entry.interactions.anime.download

import android.app.Application
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.VideoAudioTrack
import eu.kanade.tachiyomi.source.entry.VideoRequest
import eu.kanade.tachiyomi.source.entry.VideoStream
import eu.kanade.tachiyomi.source.entry.VideoStreamType
import eu.kanade.tachiyomi.source.entry.VideoSubtitle
import eu.kanade.tachiyomi.util.lang.Hash.md5
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import mihon.entry.interactions.anime.download.model.AnimeDownload
import mihon.entry.interactions.anime.download.model.AnimeDownloadFailure
import mihon.entry.interactions.anime.download.model.AnimeDownloadManifest
import mihon.entry.interactions.anime.download.model.DownloadedAudioTrack
import mihon.entry.interactions.anime.download.model.DownloadedSubtitle
import mihon.entry.interactions.anime.download.model.DownloadedVideo
import mihon.entry.interactions.anime.stream.PreferredHeight
import mihon.entry.interactions.anime.stream.firstPlayableStream
import mihon.entry.interactions.anime.stream.pickPreferredSourceQuality
import mihon.entry.interactions.anime.stream.playableCandidates
import mihon.entry.interactions.anime.stream.rankStreams
import mihon.entry.interactions.anime.stream.resolveDeferredStream
import mihon.entry.interactions.anime.stream.streamChoiceKey
import mihon.entry.interactions.anime.stream.streamDisplayLabel
import mihon.video.engine.spi.loading.resolvedRequest
import mihon.video.engine.spi.tracks.audioChoiceKey
import mihon.video.engine.spi.tracks.subtitleChoiceKey
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import tachiyomi.core.common.preference.SourcePreferenceProfile
import tachiyomi.domain.entry.adapter.toSEntryChapter
import tachiyomi.domain.entry.model.DownloadPreferences
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.VideoDownloadQualityMode
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.source.service.awaitInitialized
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

internal class AnimeDownloader(
    private val application: Application,
    private val provider: AnimeDownloadProvider,
    private val cache: AnimeDownloadCache,
    private val sourceManager: SourceManager,
    private val networkHelper: NetworkHelper,
    private val json: Json,
) {

    private val fileTransferClient = createFileTransferClient(networkHelper.client)

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
            return AnimeDownloadFailure(AnimeDownloadFailure.Reason.STREAM_NOT_AVAILABLE)
        }
        val sourceEpisode = download.episode.toSEntryChapter()
        // Resolved just before downloading, since resolved links expire.
        val stream = firstPlayableStream(rankedStreams, explicitPick = download.preferences.streamKey != null) {
            resolveDeferredStream(source, sourceEpisode, it)
        }.getOrElse {
            return AnimeDownloadFailure(reason = AnimeDownloadFailure.Reason.NETWORK, message = it.message)
        }
        val streamType = effectiveStreamType(stream)
        if (streamType != VideoStreamType.PROGRESSIVE && streamType != VideoStreamType.HLS) {
            return AnimeDownloadFailure(AnimeDownloadFailure.Reason.UNSUPPORTED_STREAM)
        }

        val subtitles = download.preferences.subtitleKey?.let { selectedKey ->
            val selected = stream.subtitles
                .filter { it.request.url.isNotBlank() }
                .firstOrNull { subtitleKey(it) == selectedKey }
                ?: return AnimeDownloadFailure(AnimeDownloadFailure.Reason.SUBTITLE_NOT_AVAILABLE)
            listOf(selected)
        }.orEmpty()
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
            download.stream = stream.copy(type = streamType)
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
            return AnimeDownloadFailure(
                reason = when {
                    e is UnsupportedOperationException -> AnimeDownloadFailure.Reason.UNSUPPORTED_STREAM
                    e is IOException -> AnimeDownloadFailure.Reason.NETWORK
                    else -> AnimeDownloadFailure.Reason.UNKNOWN
                },
                message = e.message,
            )
        }
    }

    private suspend fun downloadVideo(download: AnimeDownload, stream: VideoStream, tmpDir: UniFile): String {
        val progressTracker = VideoDownloadProgressTracker(download)
        return when (effectiveStreamType(stream)) {
            VideoStreamType.PROGRESSIVE -> {
                val extension = inferExtension(stream.resolvedRequest.url, stream.mimeType, fallback = "mp4")
                val fileName = "video.$extension"
                val file = tmpDir.createFile(fileName) ?: error("Failed to create video file")
                fetchToFile(stream.resolvedRequest, file, progressTracker = progressTracker)
                progressTracker.onVideoTransferComplete()
                fileName
            }
            VideoStreamType.HLS -> {
                downloadHls(stream.resolvedRequest, VIDEO_HLS_ROOT_PLAYLIST, tmpDir, progressTracker)
            }
            else -> {
                throw UnsupportedOperationException("Unsupported stream type: ${stream.type}")
            }
        }
    }

    private suspend fun downloadAudioTrack(track: VideoAudioTrack, tmpDir: UniFile): DownloadedAudioTrack {
        val isHls = track.type == VideoStreamType.HLS ||
            track.request.url.substringBefore('?').endsWith(".m3u8", ignoreCase = true)
        val fileName = if (isHls) {
            downloadHls(track.request, "$AUDIO_FILE_NAME.m3u8", tmpDir, progressTracker = null)
        } else {
            val name = "$AUDIO_FILE_NAME.${inferExtension(track.request.url, track.mimeType, fallback = "m4a")}"
            val file = tmpDir.createFile(name) ?: error("Failed to create audio file")
            fetchToFile(track.request, file)
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
        val extension = inferExtension(subtitle.request.url, subtitle.mimeType, fallback = "vtt")
        val fileName = "subtitle_${index + 1}.$extension"
        val file = tmpDir.createFile(fileName) ?: error("Failed to create subtitle file")
        fetchToFile(subtitle.request, file)
        return DownloadedSubtitle(
            key = subtitleKey(subtitle),
            label = subtitle.label,
            language = subtitle.language,
            mimeType = subtitle.mimeType,
            fileName = fileName,
            isDefault = subtitle.isDefault,
            isForced = subtitle.isForced,
        )
    }

    private suspend fun fetchToFile(
        request: VideoRequest,
        file: UniFile,
        progressTracker: VideoDownloadProgressTracker? = null,
    ) {
        val headers = Headers.Builder().apply {
            request.headers.forEach { (key, value) -> add(key, value) }
        }.build()
        val response = fileTransferClient.newCall(
            Request.Builder()
                .url(request.url)
                .headers(headers)
                .build(),
        ).awaitSuccess()
        response.use {
            val body = it.body
            val contentLength = body.contentLength().takeIf { it > 0L }
            progressTracker?.onResponseStarted(contentLength)
            body.source().use { source ->
                file.openOutputStream().use { output ->
                    val sink = output.sink().buffer()
                    var totalRead = 0L
                    while (true) {
                        val read = source.read(sink.buffer, 8_192)
                        if (read == -1L) break
                        sink.emit()
                        totalRead += read
                        progressTracker?.onBytesCopied(read)
                    }
                    sink.flush()
                    progressTracker?.onResponseFinished(contentLength ?: totalRead)
                }
            }
        }
    }

    private suspend fun fetchText(request: VideoRequest): String {
        val headers = Headers.Builder().apply {
            request.headers.forEach { (key, value) -> add(key, value) }
        }.build()
        val response = fileTransferClient.newCall(
            Request.Builder()
                .url(request.url)
                .headers(headers)
                .build(),
        ).awaitSuccess()
        response.use {
            return it.body.string()
        }
    }

    /** Downloads the HLS playlist [request] names with everything it references; [progressTracker] is the video's. */
    private suspend fun downloadHls(
        request: VideoRequest,
        rootPlaylistName: String,
        tmpDir: UniFile,
        progressTracker: VideoDownloadProgressTracker?,
    ): String {
        progressTracker?.markAsHls()
        val downloaded = LinkedHashMap<String, String>()
        val stagingDir =
            File(
                application.cacheDir,
                "anime_hls_${md5(request.url).take(8)}_${System.currentTimeMillis()}",
            )
        stagingDir.mkdirs()
        return try {
            val rootFileName = downloadHlsPlaylist(
                request,
                stagingDir,
                downloaded,
                progressTracker,
                rootFileName = rootPlaylistName,
            )
            progressTracker?.onVideoTransferComplete()
            var resolvedRootFileName = rootFileName
            val stagedFiles = stagingDir.listFiles().orEmpty()
            if (stagedFiles.isNotEmpty()) {
                val stagedToActualNames = LinkedHashMap<String, String>(stagedFiles.size)
                val copyStart = 90
                val copyEnd = 94
                stagedFiles.forEachIndexed { index, stagedFile ->
                    val copyFraction = (index + 1).toFloat() / stagedFiles.size.toFloat()
                    val targetProgress = copyStart + ((copyEnd - copyStart) * copyFraction).toInt()
                    progressTracker?.onNonVideoPhaseProgress(targetProgress)
                    val target = tmpDir.createFile(stagedFile.name) ?: error("Failed to create staged HLS file")
                    val actualName = target.name ?: stagedFile.name
                    stagedToActualNames[stagedFile.name] = actualName
                    stagedFile.inputStream().use { input ->
                        target.openOutputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (stagedFile.name == rootFileName) {
                        resolvedRootFileName = actualName
                    }
                }

                if (stagedToActualNames.any { (stagedName, actualName) -> stagedName != actualName }) {
                    rewriteDownloadedHlsPlaylists(tmpDir, stagedToActualNames)
                }
            }
            resolvedRootFileName
        } finally {
            stagingDir.deleteRecursively()
        }
    }

    private suspend fun downloadHlsPlaylist(
        request: VideoRequest,
        stagingDir: File,
        downloaded: MutableMap<String, String>,
        progressTracker: VideoDownloadProgressTracker?,
        rootFileName: String? = null,
    ): String {
        downloaded[request.url]?.let { return it }

        val baseUrl = request.url.toHttpUrlOrNull() ?: error("Invalid HLS url: ${request.url}")
        val playlistText = fetchText(request)
        val fileName = rootFileName ?: uniqueLocalName(request.url, fallback = "playlist.m3u8")
        downloaded[request.url] = fileName

        var assetsInPlaylist = 0
        var previousTagLine: String? = null
        for (line in playlistText.lines()) {
            val trimmed = line.trim()
            when {
                trimmed.isBlank() -> continue
                !trimmed.startsWith("#") -> {
                    val absoluteUrl = baseUrl.resolve(trimmed)?.toString() ?: trimmed
                    if (!isHlsPlaylistReference(absoluteUrl, previousTagLine = previousTagLine)) {
                        assetsInPlaylist++
                    }
                }
                "URI=\"" in trimmed -> {
                    val match = URI_ATTRIBUTE_REGEX.find(line)
                    if (match != null) {
                        val absoluteUrl = baseUrl.resolve(match.groupValues[1])?.toString() ?: match.groupValues[1]
                        if (!isHlsPlaylistReference(absoluteUrl, currentTagLine = trimmed)) {
                            assetsInPlaylist++
                        }
                    }
                }
            }
            if (trimmed.startsWith("#")) {
                previousTagLine = trimmed
            }
        }
        progressTracker?.onAssetsFound(assetsInPlaylist)

        previousTagLine = null
        val rewrittenLines = buildList {
            for (line in playlistText.lineSequence()) {
                val trimmed = line.trim()
                val rewrittenLine = when {
                    trimmed.isBlank() -> line
                    !trimmed.startsWith("#") -> {
                        val absoluteUrl = baseUrl.resolve(trimmed)?.toString() ?: trimmed
                        if (isHlsPlaylistReference(absoluteUrl, previousTagLine = previousTagLine)) {
                            downloadHlsPlaylist(
                                request = VideoRequest(url = absoluteUrl, headers = request.headers),
                                stagingDir = stagingDir,
                                downloaded = downloaded,
                                progressTracker = progressTracker,
                            )
                        } else {
                            downloadHlsAsset(absoluteUrl, request.headers, stagingDir, downloaded, progressTracker)
                        }
                    }
                    "URI=\"" in trimmed -> {
                        val match = URI_ATTRIBUTE_REGEX.find(line)
                        if (match != null) {
                            val absoluteUrl = baseUrl.resolve(match.groupValues[1])?.toString() ?: match.groupValues[1]
                            val localName = if (isHlsPlaylistReference(absoluteUrl, currentTagLine = trimmed)) {
                                downloadHlsPlaylist(
                                    request = VideoRequest(url = absoluteUrl, headers = request.headers),
                                    stagingDir = stagingDir,
                                    downloaded = downloaded,
                                    progressTracker = progressTracker,
                                )
                            } else {
                                downloadHlsAsset(absoluteUrl, request.headers, stagingDir, downloaded, progressTracker)
                            }
                            line.replace(match.value, "URI=\"$localName\"")
                        } else {
                            line
                        }
                    }
                    else -> line
                }
                add(rewrittenLine)
                if (trimmed.startsWith("#")) {
                    previousTagLine = trimmed
                }
            }
        }
        val rewritten = rewrittenLines.joinToString("\n")

        val file = File(stagingDir, fileName)
        file.outputStream().bufferedWriter().use { writer ->
            writer.write(rewritten)
        }
        return fileName
    }

    private suspend fun downloadHlsAsset(
        url: String,
        headers: Map<String, String>,
        stagingDir: File,
        downloaded: MutableMap<String, String>,
        progressTracker: VideoDownloadProgressTracker?,
    ): String {
        downloaded[url]?.let {
            progressTracker?.onAssetDownloaded()
            return it
        }
        val fileName = uniqueLocalName(url)
        val file = File(stagingDir, fileName)
        if (url.isHlsVideoAssetUrl()) {
            fetchToFile(VideoRequest(url = url, headers = headers), file, progressTracker = progressTracker)
        } else {
            fetchToFile(VideoRequest(url = url, headers = headers), file)
        }
        downloaded[url] = fileName
        progressTracker?.onAssetDownloaded()
        return fileName
    }

    private fun rewriteDownloadedHlsPlaylists(
        tmpDir: UniFile,
        stagedToActualNames: Map<String, String>,
    ) {
        stagedToActualNames.forEach { (stagedName, actualName) ->
            if (!actualName.isHlsPlaylistFileName()) return@forEach

            val playlistFile = tmpDir.findFile(actualName) ?: return@forEach
            val playlistText = playlistFile.openInputStream().bufferedReader().use { it.readText() }
            val rewrittenText = rewriteHlsPlaylistReferences(playlistText, stagedToActualNames)
            if (rewrittenText == playlistText) return@forEach

            playlistFile.openOutputStream().use { output ->
                output.writer().use { writer ->
                    writer.write(rewrittenText)
                }
            }
        }
    }

    private suspend fun fetchToFile(
        request: VideoRequest,
        file: File,
        progressTracker: VideoDownloadProgressTracker? = null,
    ) {
        val headers = Headers.Builder().apply {
            request.headers.forEach { (key, value) -> add(key, value) }
        }.build()
        val response = networkHelper.client.newCall(
            Request.Builder()
                .url(request.url)
                .headers(headers)
                .build(),
        ).awaitSuccess()
        response.use {
            val body = it.body
            val contentLength = body.contentLength().takeIf { it > 0L }
            progressTracker?.onResponseStarted(contentLength)
            file.outputStream().use { output ->
                val sink = output.sink().buffer()
                body.source().use { source ->
                    var totalRead = 0L
                    while (true) {
                        val read = source.read(sink.buffer, 8_192)
                        if (read == -1L) break
                        sink.emit()
                        totalRead += read
                        progressTracker?.onBytesCopied(read)
                    }
                    sink.flush()
                    progressTracker?.onResponseFinished(contentLength ?: totalRead)
                }
            }
        }
    }

    private fun uniqueLocalName(url: String, fallback: String = inferExtension(url, mimeType = null, fallback = "bin")):
        String {
        val path = url.substringBefore('?').substringBefore('#').substringAfterLast('/')
        val baseName = path.ifBlank { fallback }
        val sanitized = baseName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return "${md5(url).take(8)}_$sanitized"
    }

    private fun subtitleKey(subtitle: VideoSubtitle): String {
        return subtitleChoiceKey(subtitle)
    }

    private fun inferExtension(url: String, mimeType: String?, fallback: String): String {
        val mimeExtension = when {
            mimeType?.contains("mp4", ignoreCase = true) == true -> "mp4"
            mimeType?.contains("webm", ignoreCase = true) == true -> "webm"
            mimeType?.contains("mpeg", ignoreCase = true) == true -> "mp4"
            mimeType?.contains("vtt", ignoreCase = true) == true -> "vtt"
            mimeType?.contains("srt", ignoreCase = true) == true -> "srt"
            mimeType?.contains("ass", ignoreCase = true) == true -> "ass"
            else -> null
        }
        if (mimeExtension != null) return mimeExtension

        val path = url.substringBefore('?').substringBefore('#')
        val urlExtension = path.substringAfterLast('.', missingDelimiterValue = "")
        return urlExtension.takeIf { it.isNotBlank() && it.length <= 5 } ?: fallback
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
                streamType = effectiveStreamType(stream),
                mimeType = stream.mimeType,
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
        private const val VIDEO_HLS_ROOT_PLAYLIST = "video.m3u8"

        // Numbered like the subtitle files, though a download keeps one audio track for now.
        private const val AUDIO_FILE_NAME = "audio_1"
        private val URI_ATTRIBUTE_REGEX = Regex("URI=\"([^\"]+)\"")
    }
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
    val progressiveStreams = streams.filter { effectiveStreamType(it) == VideoStreamType.PROGRESSIVE }
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

internal fun effectiveStreamType(stream: VideoStream): VideoStreamType {
    if (stream.type != VideoStreamType.UNKNOWN) return stream.type

    val url = stream.request?.url.orEmpty().substringBefore('?').substringBefore('#')
    val mime = stream.mimeType.orEmpty()
    return when {
        url.endsWith(".m3u8", ignoreCase = true) -> VideoStreamType.HLS
        url.endsWith(".mpd", ignoreCase = true) -> VideoStreamType.DASH
        mime.contains("mpegurl", ignoreCase = true) || mime.contains("m3u8", ignoreCase = true) -> VideoStreamType.HLS
        mime.contains("dash", ignoreCase = true) || mime.contains("mpd", ignoreCase = true) -> VideoStreamType.DASH
        mime.contains(
            "mp4",
            ignoreCase = true,
        ) || mime.contains("webm", ignoreCase = true) -> VideoStreamType.PROGRESSIVE
        else -> VideoStreamType.UNKNOWN
    }
}

private fun String.isHlsPlaylistUrl(): Boolean {
    return substringBefore('?').substringBefore('#').endsWith(".m3u8", ignoreCase = true)
}

private fun String.isHlsPlaylistFileName(): Boolean {
    return endsWith(".m3u8", ignoreCase = true) || endsWith(".m3u", ignoreCase = true)
}

internal fun rewriteHlsPlaylistReferences(
    playlistText: String,
    stagedToActualNames: Map<String, String>,
): String {
    return stagedToActualNames.entries.fold(playlistText) { rewritten, (stagedName, actualName) ->
        if (stagedName == actualName) {
            rewritten
        } else {
            rewritten.replace(stagedName, actualName)
        }
    }
}

internal fun isHlsPlaylistReference(
    url: String,
    previousTagLine: String? = null,
    currentTagLine: String? = null,
): Boolean {
    if (url.isHlsPlaylistUrl()) return true

    return when {
        previousTagLine?.startsWith("#EXT-X-STREAM-INF", ignoreCase = true) == true -> true
        currentTagLine?.startsWith("#EXT-X-MEDIA", ignoreCase = true) == true -> {
            currentTagLine.contains("TYPE=AUDIO", ignoreCase = true) ||
                currentTagLine.contains("TYPE=VIDEO", ignoreCase = true)
        }
        currentTagLine?.startsWith("#EXT-X-I-FRAME-STREAM-INF", ignoreCase = true) == true -> true
        currentTagLine?.startsWith("#EXT-X-IMAGE-STREAM-INF", ignoreCase = true) == true -> true
        else -> false
    }
}

private class VideoDownloadProgressTracker(
    private val download: AnimeDownload,
) {
    private var totalBytesExpected = 0L
    private var downloadedBytes = 0L
    private var transferCompleted = false
    private var isHls = false
    private var totalAssetsExpected = 0
    private var downloadedAssets = 0

    fun markAsHls() {
        isHls = true
    }

    fun onAssetsFound(count: Int) {
        totalAssetsExpected += count
        update()
    }

    fun onAssetDownloaded() {
        downloadedAssets++
        update()
    }

    fun onResponseStarted(contentLength: Long?) {
        if (isHls) return
        if (contentLength != null) {
            totalBytesExpected += contentLength
            update()
        }
    }

    fun onBytesCopied(read: Long) {
        if (isHls) return
        downloadedBytes += read
        update()
    }

    fun onResponseFinished(contentLengthOrActual: Long) {
        if (isHls) return
        if (totalBytesExpected < downloadedBytes) {
            totalBytesExpected = downloadedBytes
        }
        if (totalBytesExpected == 0L) {
            totalBytesExpected = contentLengthOrActual
        }
        update()
    }

    fun onVideoTransferComplete() {
        transferCompleted = true
        download.progress = maxOf(download.progress, 90)
    }

    fun onNonVideoPhaseProgress(progress: Int) {
        transferCompleted = true
        download.progress = maxOf(download.progress, progress)
    }

    private fun update() {
        if (transferCompleted) return
        val fraction = if (isHls) {
            if (totalAssetsExpected <= 0) 0.0 else downloadedAssets.toDouble() / totalAssetsExpected.toDouble()
        } else {
            if (totalBytesExpected <= 0L) return
            downloadedBytes.toDouble() / totalBytesExpected.toDouble()
        }
        val next = (fraction.coerceIn(0.0, 1.0) * 90.0).toInt().coerceIn(0, 90)
        download.progress = maxOf(download.progress, next)
    }
}

private fun String.isHlsVideoAssetUrl(): Boolean {
    val path = substringBefore('?').substringBefore('#')
    return path.endsWith(".ts", ignoreCase = true) ||
        path.endsWith(".m4s", ignoreCase = true) ||
        path.endsWith(".mp4", ignoreCase = true) ||
        path.endsWith(".m4v", ignoreCase = true) ||
        path.endsWith(".aac", ignoreCase = true)
}

internal fun createFileTransferClient(client: OkHttpClient): OkHttpClient {
    return client.newBuilder()
        .callTimeout(0, TimeUnit.MILLISECONDS)
        .build()
}
