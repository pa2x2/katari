package mihon.entry.interactions.book

import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.source.entry.BookResourceLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream

/** Android implementation of Katari-owned external BOOK resource access. */
internal class AndroidBookExternalResourceResolver(
    context: Context,
    private val httpClient: OkHttpClient,
    private val appReferenceResolver: BookAppReferenceResolver? = null,
) : BookExternalResourceResolver {
    private val contentResolver = context.applicationContext.contentResolver
    override val canResolveAppReferences: Boolean = appReferenceResolver != null

    override suspend fun open(
        location: BookResourceLocation,
        range: BookByteRange?,
    ): ExternalBookResource {
        return when (location) {
            is BookResourceLocation.RemoteRequest -> openRemote(location, range)
            is BookResourceLocation.LocalUri -> openLocal(location, range)
            is BookResourceLocation.AppReference -> {
                val resolver = checkNotNull(appReferenceResolver) {
                    "No app-reference resolver is registered for BOOK resource ${location.id}"
                }
                resolver.open(location.id, range)
            }
            is BookResourceLocation.InlineBytes,
            is BookResourceLocation.InlineText,
            is BookResourceLocation.SourceChild,
            -> error("Location must be resolved inside the BOOK content session: $location")
        }
    }

    private suspend fun openRemote(
        location: BookResourceLocation.RemoteRequest,
        range: BookByteRange?,
    ): ExternalBookResource {
        val request = Request.Builder()
            .url(location.url)
            .apply {
                location.headers.forEach(::header)
                range?.let { header("Range", it.toHttpRange()) }
            }
            .build()
        val response = httpClient.newCall(request).awaitSuccess()
        return try {
            val responseStream = response.body.byteStream()
            val rangedStream = when {
                range == null -> responseStream
                response.code == HTTP_PARTIAL_CONTENT -> {
                    response.requireMatchingContentRange(range)
                    responseStream.limitTo(range.length)
                }
                else -> withContext(Dispatchers.IO) {
                    currentCoroutineContext().ensureActive()
                    responseStream.slice(range)
                }
            }
            ResponseExternalBookResource(response, rangedStream)
        } catch (error: Throwable) {
            response.close()
            throw error
        }
    }

    private suspend fun openLocal(
        location: BookResourceLocation.LocalUri,
        range: BookByteRange?,
    ): ExternalBookResource = withContext(Dispatchers.IO) {
        val stream = contentResolver.openInputStream(Uri.parse(location.uri))
            ?: throw IOException("Unable to open BOOK content URI")
        try {
            currentCoroutineContext().ensureActive()
            SimpleAndroidExternalBookResource(if (range == null) stream else stream.slice(range))
        } catch (error: Throwable) {
            stream.close()
            throw error
        }
    }

    private companion object {
        const val HTTP_PARTIAL_CONTENT = 206
    }
}

/** Resolves opaque references to Katari-owned cache or download resources. */
internal interface BookAppReferenceResolver {
    suspend fun open(id: String, range: BookByteRange?): ExternalBookResource
}

private class ResponseExternalBookResource(
    private val response: Response,
    override val stream: InputStream,
) : ExternalBookResource {
    private var closed = false

    override fun close() {
        synchronized(this) {
            if (closed) return
            closed = true
        }
        response.close()
    }
}

private class SimpleAndroidExternalBookResource(
    override val stream: InputStream,
) : ExternalBookResource {
    override fun close() = stream.close()
}

private fun BookByteRange.toHttpRange(): String = buildString {
    append("bytes=")
    append(startInclusive)
    append('-')
    endExclusive?.let { append(it - 1) }
}

private val BookByteRange.length: Long?
    get() = endExclusive?.minus(startInclusive)

private fun Response.requireMatchingContentRange(range: BookByteRange) {
    val contentRange = header("Content-Range")
        ?: throw IOException("Partial BOOK response omitted Content-Range")
    val start = contentRange
        .substringAfter("bytes ", missingDelimiterValue = "")
        .substringBefore('-')
        .toLongOrNull()
    if (start != range.startInclusive) {
        throw IOException("Partial BOOK response starts at an unexpected offset")
    }
}

private fun InputStream.slice(range: BookByteRange): InputStream {
    skipFully(range.startInclusive)
    return limitTo(range.length)
}

private fun InputStream.limitTo(length: Long?): InputStream {
    return if (length == null) this else LimitedInputStream(this, length)
}

private fun InputStream.skipFully(byteCount: Long) {
    var remaining = byteCount
    while (remaining > 0L) {
        val skipped = skip(remaining)
        if (skipped > 0L) {
            remaining -= skipped
        } else if (read() >= 0) {
            remaining--
        } else {
            throw IOException("BOOK range starts beyond the resource")
        }
    }
}

private class LimitedInputStream(
    delegate: InputStream,
    private var remaining: Long,
) : FilterInputStream(delegate) {
    override fun read(): Int {
        if (remaining == 0L) return -1
        return super.read().also { read ->
            if (read >= 0) remaining--
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (remaining == 0L) return -1
        val boundedLength = minOf(length.toLong(), remaining).toInt()
        return super.read(buffer, offset, boundedLength).also { read ->
            if (read > 0) remaining -= read
        }
    }

    override fun skip(byteCount: Long): Long {
        val skipped = super.skip(minOf(byteCount, remaining))
        remaining -= skipped
        return skipped
    }

    override fun available(): Int = minOf(super.available().toLong(), remaining).toInt()
}
