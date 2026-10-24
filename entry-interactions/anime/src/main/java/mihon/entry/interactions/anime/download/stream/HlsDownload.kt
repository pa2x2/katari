package mihon.entry.interactions.anime.download.stream

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.entry.VideoRequest
import eu.kanade.tachiyomi.util.lang.Hash.md5
import okhttp3.OkHttpClient

/**
 * Downloads an HLS playlist with every playlist, segment, key and map it references straight into an episode's folder,
 * the playlists rewritten to name the downloaded files. [playlistClient] fetches playlists, [assetClient] the rest.
 *
 * Playlists are written as `.m3u`: Android's shared storage gives a file it types as a playlist its type's own
 * extension, so a `.m3u8` comes out as `.m3u` and the playlist naming it would point at nothing.
 */
internal class HlsDownload(
    private val playlistClient: OkHttpClient,
    private val assetClient: OkHttpClient,
) {

    /**
     * Downloads the HLS playlist [request] names, as [rootName] with the playlist extension, with everything it
     * references, and returns its file name; [progressTracker] is the video's.
     */
    suspend fun download(
        request: VideoRequest,
        rootName: String,
        directory: UniFile,
        progressTracker: VideoDownloadProgressTracker?,
    ): String {
        progressTracker?.markAsHls()
        val rootFileName = downloadHlsPlaylist(
            request,
            directory,
            downloaded = LinkedHashMap(),
            progressTracker,
            rootFileName = "$rootName$PLAYLIST_EXTENSION",
        )
        progressTracker?.onVideoTransferComplete()
        return rootFileName
    }

    private suspend fun downloadHlsPlaylist(
        request: VideoRequest,
        directory: UniFile,
        downloaded: MutableMap<String, String>,
        progressTracker: VideoDownloadProgressTracker?,
        rootFileName: String? = null,
    ): String {
        downloaded[request.url]?.let { return it }

        val playlist = playlistClient.fetchText(request)
        val baseUrl = playlist.url
        val playlistText = playlist.text
        val fileName = rootFileName
            ?: (uniqueLocalName(request.url, fallback = "playlist").removePlaylistExtension() + PLAYLIST_EXTENSION)
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
                                directory = directory,
                                downloaded = downloaded,
                                progressTracker = progressTracker,
                            )
                        } else {
                            downloadHlsAsset(absoluteUrl, request.headers, directory, downloaded, progressTracker)
                        }
                    }
                    "URI=\"" in trimmed -> {
                        val match = URI_ATTRIBUTE_REGEX.find(line)
                        if (match != null) {
                            val absoluteUrl = baseUrl.resolve(match.groupValues[1])?.toString() ?: match.groupValues[1]
                            val localName = if (isHlsPlaylistReference(absoluteUrl, currentTagLine = trimmed)) {
                                downloadHlsPlaylist(
                                    request = VideoRequest(url = absoluteUrl, headers = request.headers),
                                    directory = directory,
                                    downloaded = downloaded,
                                    progressTracker = progressTracker,
                                )
                            } else {
                                downloadHlsAsset(absoluteUrl, request.headers, directory, downloaded, progressTracker)
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

        directory.createNamedFile(fileName).openOutputStream().bufferedWriter().use { writer ->
            writer.write(rewritten)
        }
        return fileName
    }

    private suspend fun downloadHlsAsset(
        url: String,
        headers: Map<String, String>,
        directory: UniFile,
        downloaded: MutableMap<String, String>,
        progressTracker: VideoDownloadProgressTracker?,
    ): String {
        downloaded[url]?.let {
            progressTracker?.onAssetDownloaded()
            return it
        }
        val fileName = uniqueLocalName(url)
        val file = directory.createNamedFile(fileName)
        val assetProgress = progressTracker.takeIf { url.isHlsVideoAssetUrl() }
        assetClient.fetchInto(VideoRequest(url = url, headers = headers), file::openOutputStream, assetProgress)
        downloaded[url] = fileName
        progressTracker?.onAssetDownloaded()
        return fileName
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

private fun String.removePlaylistExtension(): String =
    replace(Regex("""\.m3u8?$""", RegexOption.IGNORE_CASE), "")

private const val PLAYLIST_EXTENSION = ".m3u"
private val URI_ATTRIBUTE_REGEX = Regex("URI=\"([^\"]+)\"")
