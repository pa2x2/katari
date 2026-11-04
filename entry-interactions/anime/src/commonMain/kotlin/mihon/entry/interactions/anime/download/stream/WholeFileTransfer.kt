package mihon.entry.interactions.anime.download.stream

import eu.kanade.tachiyomi.source.entry.VideoRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import okhttp3.OkHttpClient
import tachiyomi.core.common.storage.StorageFileResolver
import java.io.FileNotFoundException
import java.io.OutputStream

/**
 * Copies one whole file a stream or track names into a download. A source may hand over a file it made itself as a
 * `file:` or `content:` URI (KissKH decrypts its subtitles into a temporary file), which is read from storage; any
 * other URL is fetched with [client].
 */
internal class WholeFileTransfer(
    private val files: StorageFileResolver,
    private val client: OkHttpClient,
) {

    suspend fun copyInto(
        request: VideoRequest,
        openOutput: () -> OutputStream,
        progressTracker: VideoDownloadProgressTracker? = null,
    ) {
        if (!isLocalFile(request.url)) {
            client.fetchInto(request, openOutput, progressTracker)
            return
        }
        runInterruptible(Dispatchers.IO) {
            val input = files.fromUri(request.url)?.openInputStream() ?: throw FileNotFoundException(request.url)
            progressTracker?.onResponseStarted(contentLength = null)
            val copied = input.use { source -> openOutput().use { source.copyTo(it) } }
            progressTracker?.onBytesCopied(copied)
            progressTracker?.onResponseFinished(copied)
        }
    }
}

/** Whether [url] names a file on the device rather than one to fetch. */
internal fun isLocalFile(url: String): Boolean =
    url.substringBefore(':', missingDelimiterValue = "").lowercase() in LOCAL_FILE_SCHEMES

/**
 * A playlist or manifest names its parts relative to where it came from, and parts of one on the device aren't
 * resolved, so only a whole file there is copied.
 */
internal fun requireRemoteManifest(request: VideoRequest) {
    if (isLocalFile(request.url)) {
        throw UnsupportedOperationException("A playlist on the device can't be downloaded: ${request.url}")
    }
}

private val LOCAL_FILE_SCHEMES = setOf("file", "content")
