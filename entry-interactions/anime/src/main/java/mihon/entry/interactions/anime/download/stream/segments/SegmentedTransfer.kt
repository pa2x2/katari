package mihon.entry.interactions.anime.download.stream.segments

import eu.kanade.tachiyomi.source.entry.VideoRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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
 *
 * A few files are fetched at once, in the plan's order, since a segment is small and most of its time goes to the
 * request's round trip. Each file has its own name, so the order they finish in changes nothing on disk. The first
 * failure cancels the rest and is the download's.
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
        val pending = Channel<SegmentFile>(Channel.UNLIMITED)
        plan.files.forEach { file ->
            val keptSize = completed[file.name]
            if (keptSize != null) progressTracker?.onFileKept(keptSize) else pending.trySend(file)
        }
        pending.close()

        coroutineScope {
            repeat(CONCURRENT_FETCHES) {
                launch(Dispatchers.IO) {
                    for (file in pending) {
                        val size = client.fetchInto(
                            request = VideoRequest(url = file.url.toString(), headers = headers),
                            openOutput = staging.newFile(file.name)::openOutputStream,
                            progressTracker = progressTracker,
                            range = file.range,
                        )
                        staging.recordCompleted(plan.part, file.name, size)
                        progressTracker?.onAssetDownloaded()
                    }
                }
            }
        }
        plan.texts.forEach { (name, text) ->
            staging.newFile(name).openOutputStream().use { it.write(text.toByteArray()) }
        }
        progressTracker?.onVideoTransferComplete()
    }

    private companion object {
        const val CONCURRENT_FETCHES = 4
    }
}
