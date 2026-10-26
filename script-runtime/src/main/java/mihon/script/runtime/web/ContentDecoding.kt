package mihon.script.runtime.web

import okhttp3.CompressionInterceptor.DecompressionAlgorithm
import okhttp3.Gzip
import okhttp3.Response
import okhttp3.brotli.Brotli
import okhttp3.zstd.Zstd
import okio.BufferedSource
import okio.InflaterSource
import okio.Source
import okio.buffer
import java.util.zip.Inflater

/*
 * OkHttp undoes gzip only when it asked for it itself, and scripts often send their own Accept-Encoding (LNReader's
 * default headers ask for "gzip, deflate"). Scripts expect decoded bodies, as browsers and LNReader's Expo fetch
 * give them, so whatever Content-Encoding is left is undone here. Encodings nobody can decode pass through as Expo
 * passes them.
 */

/** This response's body with its remaining Content-Encoding undone, and whether anything was undone. */
internal fun Response.decodedBody(): Pair<BufferedSource, Boolean> {
    val source = body.source()
    val encodings = header("Content-Encoding").orEmpty()
        .split(',')
        .map { it.trim().lowercase() }
        .filter { it.isNotEmpty() && it != "identity" }
    if (encodings.isEmpty() || encodings.any { it !in DECODERS }) return source to false
    // Listed in the order they were applied
    return encodings.foldRight(source) { encoding, encoded ->
        DECODERS.getValue(encoding).decompress(encoded).buffer()
    } to
        true
}

private object Deflate : DecompressionAlgorithm {
    override val encoding: String get() = "deflate"

    override fun decompress(compressedSource: BufferedSource): Source = InflaterSource(compressedSource, Inflater())
}

private val DECODERS = listOf(Gzip, Deflate, Brotli, Zstd).associateBy { it.encoding } + ("x-gzip" to Gzip)
