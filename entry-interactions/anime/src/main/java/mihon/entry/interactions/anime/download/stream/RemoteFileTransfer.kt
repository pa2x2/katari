package mihon.entry.interactions.anime.download.stream

import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.source.entry.VideoRequest
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.TimeUnit

/**
 * Writes the body of [request], or only the bytes [range] of it, to the stream [openOutput] opens, reporting its bytes
 * to [progressTracker], and returns how many it wrote.
 */
internal suspend fun OkHttpClient.fetchInto(
    request: VideoRequest,
    openOutput: () -> OutputStream,
    progressTracker: VideoDownloadProgressTracker? = null,
    range: LongRange? = null,
): Long {
    val httpRequest = request.toHttpRequest().let { httpRequest ->
        range?.let { httpRequest.newBuilder().header("Range", "bytes=${it.first}-${it.last}").build() } ?: httpRequest
    }
    return newCall(httpRequest).awaitSuccess().use {
        // A server that ignores the range sends the whole file, which would be stored as the part.
        if (range != null && it.code != HTTP_PARTIAL_CONTENT) throw IOException("No byte range from ${request.url}")
        val body = it.body
        val contentLength = body.contentLength().takeIf { it > 0L }
        progressTracker?.onResponseStarted(contentLength)
        body.source().use { source ->
            openOutput().use { output ->
                val sink = output.sink().buffer()
                var totalRead = 0L
                while (true) {
                    // The read blocks rather than suspends, so a cancelled download stops between chunks.
                    currentCoroutineContext().ensureActive()
                    val read = source.read(sink.buffer, 8_192)
                    if (read == -1L) break
                    sink.emit()
                    totalRead += read
                    progressTracker?.onBytesCopied(read)
                }
                sink.flush()
                progressTracker?.onResponseFinished(contentLength ?: totalRead)
                totalRead
            }
        }
    }
}

internal suspend fun OkHttpClient.fetchText(request: VideoRequest): RemoteText =
    newCall(request.toHttpRequest()).awaitSuccess().use { RemoteText(it.body.string(), it.request.url) }

/** A playlist or manifest, and [url], where it came from after redirects, which its references resolve against. */
internal class RemoteText(val text: String, val url: HttpUrl)

/** The client for whole files: a long episode outlasts any call timeout, so only the connect and read timeouts apply. */
internal fun createFileTransferClient(client: OkHttpClient): OkHttpClient {
    return client.newBuilder()
        .callTimeout(0, TimeUnit.MILLISECONDS)
        .build()
}

private fun VideoRequest.toHttpRequest(): Request {
    val headers = Headers.Builder().apply {
        this@toHttpRequest.headers.forEach { (key, value) -> add(key, value) }
    }.build()
    return Request.Builder()
        .url(url)
        .headers(headers)
        .build()
}

private const val HTTP_PARTIAL_CONTENT = 206
