package mihon.video.engine.mpv.proxy

import okhttp3.CompressionInterceptor.DecompressionAlgorithm
import okhttp3.Gzip
import okhttp3.Response
import okhttp3.brotli.Brotli
import okio.BufferedSource
import okio.InflaterSource
import okio.Source
import okio.buffer
import java.io.IOException
import java.util.zip.Inflater

/*
 * The proxy gets upstream bodies still encoded: OkHttp undoes gzip only when it asked for it itself, which it doesn't
 * for a request with a Range (FFmpeg sends `Range: bytes=0-` with every request) or with the source's own
 * Accept-Encoding, and its cache answers such a request with the encoded response it stored for one that did ask.
 * Bodies relayed as they came keep their Content-Encoding, which FFmpeg undoes; a manifest the proxy reads is decoded
 * here first.
 */

/** [source], this response's body or a peek at it, with the body's Content-Encoding undone. */
internal fun Response.decoded(source: BufferedSource): BufferedSource {
    val encodings = header("Content-Encoding").orEmpty()
        .split(',')
        .map { it.trim().lowercase() }
        .filter { it.isNotEmpty() && it != "identity" }
    // Listed in the order they were applied.
    return encodings.foldRight(source) { encoding, encoded ->
        val algorithm = DECODERS[encoding] ?: throw IOException("Unsupported content encoding '$encoding'")
        algorithm.decompress(encoded).buffer()
    }
}

private object Deflate : DecompressionAlgorithm {
    override val encoding: String get() = "deflate"

    override fun decompress(compressedSource: BufferedSource): Source = InflaterSource(compressedSource, Inflater())
}

private val DECODERS = listOf(Gzip, Deflate, Brotli).associateBy { it.encoding } + ("x-gzip" to Gzip)
