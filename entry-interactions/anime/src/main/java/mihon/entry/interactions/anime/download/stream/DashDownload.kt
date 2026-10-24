package mihon.entry.interactions.anime.download.stream

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.entry.VideoRequest
import mihon.entry.interactions.anime.stream.PreferredHeight
import mihon.video.engine.dash.UnsupportedDashManifestException
import mihon.video.engine.dash.localDashManifest
import mihon.video.engine.dash.readDashManifest
import okhttp3.OkHttpClient

/**
 * Downloads a DASH presentation into an episode's folder: the representations [chooseDashRepresentations] keeps, file
 * by file, straight into the folder under the names their local manifest gives them, then that manifest, which both
 * engines play offline. A single file can be a whole film, so [client] must have no call timeout.
 */
internal class DashDownload(private val client: OkHttpClient) {

    /** Returns the local manifest's file name; [progressTracker] is the video's. */
    suspend fun download(
        request: VideoRequest,
        preferredHeight: PreferredHeight,
        directory: UniFile,
        progressTracker: VideoDownloadProgressTracker?,
    ): String {
        requireRemoteManifest(request)
        val remote = client.fetchText(request)
        val manifest = readDashManifest(remote.text, remote.url)
        if (manifest.isDynamic) throw UndownloadableStreamException(UndownloadableStreamException.Reason.LIVE)
        if (manifest.hasRemoteElements) throw UnsupportedDashManifestException("Manifest with remote elements")
        val kept = chooseDashRepresentations(manifest, preferredHeight) { isDecodableOnDevice(it.codecs) }
        kept.keys.mapNotNull { it.estimatedBytes }
            .takeIf { it.size == kept.size }
            ?.let { progressTracker?.expectBytes(it.sum()) }

        val local = localDashManifest(manifest, kept)
        for (file in local.files) {
            val target = directory.createNamedFile(file.name)
            client.fetchInto(
                request = VideoRequest(url = file.segment.url.toString(), headers = request.headers),
                openOutput = target::openOutputStream,
                progressTracker = progressTracker,
                range = file.segment.range,
            )
        }
        directory.createNamedFile(LOCAL_MANIFEST_NAME).openOutputStream().use { it.write(local.text.toByteArray()) }
        progressTracker?.onVideoTransferComplete()
        return LOCAL_MANIFEST_NAME
    }

    private companion object {
        const val LOCAL_MANIFEST_NAME = "video.mpd"
    }
}
