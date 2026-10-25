package mihon.entry.interactions.anime.download.stream

import android.content.ContentResolver
import android.net.Uri
import eu.kanade.tachiyomi.source.entry.VideoRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import okhttp3.OkHttpClient
import java.io.FileNotFoundException
import java.io.OutputStream

/**
 * Copies one whole file a stream or track names into a download. A source may hand over a file it made itself as a
 * `file:` or `content:` URI (KissKH decrypts its subtitles into a temporary file), which is read from storage; any
 * other URL is fetched with [client].
 */
internal class WholeFileTransfer(
    private val contentResolver: ContentResolver,
    private val client: OkHttpClient,
) {

    suspend fun copyInto(
        request: VideoRequest,
        openOutput: () -> OutputStream,
        progressTracker: VideoDownloadProgressTracker? = null,
    ) {
        val uri = localFileUri(request.url)
        if (uri == null) {
            client.fetchInto(request, openOutput, progressTracker)
            return
        }
        runInterruptible(Dispatchers.IO) {
            val input = contentResolver.openInputStream(uri) ?: throw FileNotFoundException(request.url)
            progressTracker?.onResponseStarted(contentLength = null)
            val copied = input.use { source -> openOutput().use { source.copyTo(it) } }
            progressTracker?.onBytesCopied(copied)
            progressTracker?.onResponseFinished(copied)
        }
    }
}

/** The file on the device [url] names, or null when it is to be fetched. */
internal fun localFileUri(url: String): Uri? =
    Uri.parse(url).takeIf { it.scheme?.lowercase() in LOCAL_FILE_SCHEMES }

/**
 * A playlist or manifest names its parts relative to where it came from, and parts of one on the device aren't
 * resolved, so only a whole file there is copied.
 */
internal fun requireRemoteManifest(request: VideoRequest) {
    if (localFileUri(request.url) != null) {
        throw UnsupportedOperationException("A playlist on the device can't be downloaded: ${request.url}")
    }
}

private val LOCAL_FILE_SCHEMES = setOf(ContentResolver.SCHEME_FILE, ContentResolver.SCHEME_CONTENT)
