package mihon.entry.interactions.anime.download.stream.hls

import eu.kanade.tachiyomi.source.entry.VideoRequest
import mihon.entry.interactions.anime.download.staging.StagingFolder
import mihon.entry.interactions.anime.download.stream.VideoDownloadProgressTracker
import mihon.entry.interactions.anime.download.stream.fetchText
import mihon.entry.interactions.anime.download.stream.isDecodableOnDevice
import mihon.entry.interactions.anime.download.stream.requireRemoteManifest
import mihon.entry.interactions.anime.download.stream.segments.SegmentedTransfer
import mihon.entry.interactions.anime.stream.PreferredHeight
import okhttp3.OkHttpClient

/**
 * Downloads an HLS stream into an episode's staging folder: of a multivariant playlist, the variant
 * [chooseHlsVariant] keeps and its renditions; then the playlists, rewritten to name the downloaded files.
 */
internal class HlsDownload(private val client: OkHttpClient) {
    private val transfer = SegmentedTransfer(client)

    /**
     * Downloads the HLS stream [request] names as [rootName] with the playlist extension, and returns that file's
     * name; [progressTracker] is the video's.
     */
    suspend fun download(
        request: VideoRequest,
        preferredHeight: PreferredHeight,
        rootName: String,
        staging: StagingFolder,
        progressTracker: VideoDownloadProgressTracker?,
    ): String {
        requireRemoteManifest(request)
        progressTracker?.markAsHls()
        val plan = planHlsDownload(
            root = client.fetchText(request),
            preferredHeight = preferredHeight,
            isDecodable = ::isDecodableOnDevice,
            rootName = rootName,
            rootFileName = "$rootName$PLAYLIST_EXTENSION",
        ) { client.fetchText(VideoRequest(url = it.toString(), headers = request.headers)) }
        transfer.download(plan, request.headers, staging, progressTracker)
        return plan.part
    }
}
