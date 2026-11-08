package app.katari.entry.interactions.anime.download.stream.dash

import app.katari.entry.interactions.anime.download.staging.StagingFolder
import app.katari.entry.interactions.anime.download.stream.UndownloadableStreamException
import app.katari.entry.interactions.anime.download.stream.VideoDownloadProgressTracker
import app.katari.entry.interactions.anime.download.stream.fetchText
import app.katari.entry.interactions.anime.download.stream.isDecodableOnDevice
import app.katari.entry.interactions.anime.download.stream.requireRemoteManifest
import app.katari.entry.interactions.anime.download.stream.segments.SegmentFile
import app.katari.entry.interactions.anime.download.stream.segments.SegmentPlan
import app.katari.entry.interactions.anime.download.stream.segments.SegmentedTransfer
import app.katari.entry.interactions.anime.stream.PreferredHeight
import app.katari.video.engine.dash.UnsupportedDashManifestException
import app.katari.video.engine.dash.localDashManifest
import app.katari.video.engine.dash.readDashManifest
import eu.kanade.tachiyomi.source.entry.VideoRequest
import okhttp3.OkHttpClient

/**
 * Downloads a DASH presentation into an episode's staging folder: the representations [chooseDashRepresentations]
 * keeps, file by file, under the names their local manifest gives them, then that manifest, which both engines play
 * offline.
 */
internal class DashDownload(private val client: OkHttpClient) {
    private val transfer = SegmentedTransfer(client)

    /** Returns the local manifest's file name; [progressTracker] is the video's. */
    suspend fun download(
        request: VideoRequest,
        preferredHeight: PreferredHeight,
        staging: StagingFolder,
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
        val plan = SegmentPlan(
            part = LOCAL_MANIFEST_NAME,
            texts = mapOf(LOCAL_MANIFEST_NAME to local.text),
            files = local.files.map { SegmentFile(it.name, it.segment.url, it.segment.range) },
        )
        transfer.download(plan, request.headers, staging, progressTracker)
        return LOCAL_MANIFEST_NAME
    }

    private companion object {
        const val LOCAL_MANIFEST_NAME = "video.mpd"
    }
}
