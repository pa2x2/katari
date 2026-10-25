package mihon.entry.interactions.anime.download.stream.hls

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.entry.VideoRequest
import mihon.entry.interactions.anime.download.stream.VideoDownloadProgressTracker
import mihon.entry.interactions.anime.download.stream.createNamedFile
import mihon.entry.interactions.anime.download.stream.fetchInto
import mihon.entry.interactions.anime.download.stream.fetchText
import mihon.entry.interactions.anime.download.stream.isDecodableOnDevice
import mihon.entry.interactions.anime.download.stream.requireRemoteManifest
import mihon.entry.interactions.anime.stream.PreferredHeight
import okhttp3.OkHttpClient

/**
 * Downloads an HLS stream straight into an episode's folder: of a multivariant playlist, the variant
 * [chooseHlsVariant] keeps and its renditions; then the playlists, rewritten to name the downloaded files.
 * [playlistClient] fetches playlists, [assetClient] the rest.
 */
internal class HlsDownload(
    private val playlistClient: OkHttpClient,
    private val assetClient: OkHttpClient,
) {

    /**
     * Downloads the HLS stream [request] names as [rootName] with the playlist extension, and returns that file's
     * name; [progressTracker] is the video's.
     */
    suspend fun download(
        request: VideoRequest,
        preferredHeight: PreferredHeight,
        rootName: String,
        directory: UniFile,
        progressTracker: VideoDownloadProgressTracker?,
    ): String {
        requireRemoteManifest(request)
        progressTracker?.markAsHls()
        val rootFileName = "$rootName$PLAYLIST_EXTENSION"
        val plan = planHlsDownload(
            root = playlistClient.fetchText(request),
            preferredHeight = preferredHeight,
            isDecodable = ::isDecodableOnDevice,
            rootName = rootName,
            rootFileName = rootFileName,
        ) { playlistClient.fetchText(VideoRequest(url = it.toString(), headers = request.headers)) }

        progressTracker?.onAssetsFound(plan.files.size)
        for (file in plan.files) {
            assetClient.fetchInto(
                request = VideoRequest(url = file.url.toString(), headers = request.headers),
                openOutput = directory.createNamedFile(file.name)::openOutputStream,
                range = file.range,
            )
            progressTracker?.onAssetDownloaded()
        }
        plan.playlists.forEach { (name, text) ->
            directory.createNamedFile(name).openOutputStream().bufferedWriter().use { it.write(text) }
        }
        progressTracker?.onVideoTransferComplete()
        return rootFileName
    }
}
