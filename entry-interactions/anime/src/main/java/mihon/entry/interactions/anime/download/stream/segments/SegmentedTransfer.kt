package mihon.entry.interactions.anime.download.stream.segments

import eu.kanade.tachiyomi.source.entry.VideoRequest
import mihon.entry.interactions.anime.download.staging.StagingFolder
import mihon.entry.interactions.anime.download.stream.VideoDownloadProgressTracker
import mihon.entry.interactions.anime.download.stream.fetchInto
import okhttp3.OkHttpClient

/**
 * Downloads a [SegmentPlan] into a [StagingFolder], resuming an earlier attempt: the files it completed under the same
 * plan are kept and only the rest is fetched, each recorded once it is whole; then the plan's playlists or manifest
 * are written. Links expire and a stream may change between attempts, so the plan is made from the stream resolved
 * again and matched by place, never by URL; a plan that differs starts over. A segment can be a whole film, so
 * [client] must have no call timeout.
 */
internal class SegmentedTransfer(private val client: OkHttpClient) {

    /** [headers] go with every request; [progressTracker] is the video's. */
    suspend fun download(
        plan: SegmentPlan,
        headers: Map<String, String>,
        staging: StagingFolder,
        progressTracker: VideoDownloadProgressTracker?,
    ) {
        val completed = staging.completedFiles(plan.part, plan.id)
        progressTracker?.onAssetsFound(plan.files.size)
        for (file in plan.files) {
            val keptSize = completed[file.name]
            if (keptSize != null) {
                progressTracker?.onFileKept(keptSize)
                continue
            }
            val size = client.fetchInto(
                request = VideoRequest(url = file.url.toString(), headers = headers),
                openOutput = staging.newFile(file.name)::openOutputStream,
                progressTracker = progressTracker,
                range = file.range,
            )
            staging.recordCompleted(plan.part, file.name, size)
            progressTracker?.onAssetDownloaded()
        }
        plan.texts.forEach { (name, text) ->
            staging.newFile(name).openOutputStream().use { it.write(text.toByteArray()) }
        }
        progressTracker?.onVideoTransferComplete()
    }
}
