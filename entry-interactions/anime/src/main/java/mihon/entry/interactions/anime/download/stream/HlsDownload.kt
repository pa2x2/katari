package mihon.entry.interactions.anime.download.stream

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.entry.VideoRequest
import eu.kanade.tachiyomi.util.lang.Hash.md5
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import java.io.File

/**
 * Downloads an HLS playlist with every playlist, segment, key and map it references into an episode's folder, the
 * playlists rewritten to name the downloaded files. Files are staged in [cacheDir] first, then copied, since the
 * storage tree may name a copy differently and the playlists are then rewritten to match. [playlistClient] fetches
 * playlists, [assetClient] the rest.
 */
internal class HlsDownload(
    private val cacheDir: File,
    private val playlistClient: OkHttpClient,
    private val assetClient: OkHttpClient,
) {

    /** Downloads the HLS playlist [request] names with everything it references; [progressTracker] is the video's. */
    suspend fun download(
        request: VideoRequest,
        rootPlaylistName: String,
        tmpDir: UniFile,
        progressTracker: VideoDownloadProgressTracker?,
    ): String {
        progressTracker?.markAsHls()
        val downloaded = LinkedHashMap<String, String>()
        val stagingDir =
            File(
                cacheDir,
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
        val playlistText = playlistClient.fetchText(request).text
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
        val assetProgress = progressTracker.takeIf { url.isHlsVideoAssetUrl() }
        assetClient.fetchInto(VideoRequest(url = url, headers = headers), file::outputStream, assetProgress)
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

    private fun uniqueLocalName(
        url: String,
        fallback: String = mediaFileExtension(url, mimeType = null, fallback = "bin"),
    ): String {
        val path = url.substringBefore('?').substringBefore('#').substringAfterLast('/')
        val baseName = path.ifBlank { fallback }
        val sanitized = baseName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return "${md5(url).take(8)}_$sanitized"
    }
}

private fun String.isHlsPlaylistUrl(): Boolean {
    return substringBefore('?').substringBefore('#').endsWith(".m3u8", ignoreCase = true)
}

private fun String.isHlsPlaylistFileName(): Boolean {
    return endsWith(".m3u8", ignoreCase = true) || endsWith(".m3u", ignoreCase = true)
}

private fun rewriteHlsPlaylistReferences(
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

private fun String.isHlsVideoAssetUrl(): Boolean {
    val path = substringBefore('?').substringBefore('#')
    return path.endsWith(".ts", ignoreCase = true) ||
        path.endsWith(".m4s", ignoreCase = true) ||
        path.endsWith(".mp4", ignoreCase = true) ||
        path.endsWith(".m4v", ignoreCase = true) ||
        path.endsWith(".aac", ignoreCase = true)
}

private val URI_ATTRIBUTE_REGEX = Regex("URI=\"([^\"]+)\"")
