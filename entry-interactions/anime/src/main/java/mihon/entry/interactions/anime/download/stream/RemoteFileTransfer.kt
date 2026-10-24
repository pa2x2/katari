package mihon.entry.interactions.anime.download.stream

import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.source.entry.VideoRequest
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.OutputStream
import java.util.concurrent.TimeUnit

/** Writes the body of [request] to the stream [openOutput] opens, reporting its bytes to [progressTracker]. */
internal suspend fun OkHttpClient.fetchInto(
    request: VideoRequest,
    openOutput: () -> OutputStream,
    progressTracker: VideoDownloadProgressTracker? = null,
) {
    newCall(request.toHttpRequest()).awaitSuccess().use {
        val body = it.body
        val contentLength = body.contentLength().takeIf { it > 0L }
        progressTracker?.onResponseStarted(contentLength)
        body.source().use { source ->
            openOutput().use { output ->
                val sink = output.sink().buffer()
                var totalRead = 0L
                while (true) {
                    val read = source.read(sink.buffer, 8_192)
                    if (read == -1L) break
                    sink.emit()
                    totalRead += read
                    progressTracker?.onBytesCopied(read)
                }
                sink.flush()
                progressTracker?.onResponseFinished(contentLength ?: totalRead)
            }
        }
    }
}

internal suspend fun OkHttpClient.fetchText(request: VideoRequest): String =
    newCall(request.toHttpRequest()).awaitSuccess().use { it.body.string() }

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
