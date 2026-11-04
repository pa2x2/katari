package mihon.video.engine.mpv.proxy

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.net.Uri
import androidx.core.net.toFile
import androidx.core.net.toUri
import mihon.video.engine.spi.loading.siblingDocumentUri
import java.io.Closeable
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream

/** A downloaded file, from the storage tree (`content://`) or a plain path (`file://`), read for the proxy. */
internal class LocalMedia(private val context: Context, val uri: Uri) {

    fun readText(): String = when (uri.scheme) {
        "file" -> uri.toFile().readText()
        else -> context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
            ?: throw FileNotFoundException(uri.toString())
    }

    /** Opens the bytes [range] asks for, or the whole file. */
    fun open(range: ByteRange?): Opened {
        val descriptor = context.contentResolver.openAssetFileDescriptor(uri, "r")
            ?: throw FileNotFoundException(uri.toString())
        val size = descriptor.length.takeIf { it != AssetFileDescriptor.UNKNOWN_LENGTH }
            ?: descriptor.parcelFileDescriptor.statSize
        val stream = FileInputStream(descriptor.fileDescriptor)
        val start = range?.first?.coerceAtMost(size) ?: 0L
        val end = range?.last?.coerceAtMost(size - 1) ?: (size - 1)
        stream.channel.position(descriptor.startOffset + start)
        return Opened(
            stream = BoundedInputStream(stream, (end - start + 1).coerceAtLeast(0), descriptor),
            length = (end - start + 1).coerceAtLeast(0),
            contentRange = range?.let { "bytes $start-$end/$size" },
        )
    }

    class Opened(
        val stream: InputStream,
        val length: Long,
        /** Set for a range request, which is answered with 206. */
        val contentRange: String?,
    ) : Closeable {
        val partial: Boolean get() = contentRange != null

        override fun close() = stream.close()
    }

    companion object {
        /** [name] next to [file]: a sibling document in its storage tree, or a sibling path. */
        fun sibling(file: Uri, name: String): Uri? = when (file.scheme) {
            "file" -> file.toFile().resolveSibling(name).toUri()
            else -> siblingDocumentUri(file, name)
        }
    }
}

/** Reads at most [limit] bytes of [source], then closes [owner] with it. */
private class BoundedInputStream(
    private val source: InputStream,
    private var limit: Long,
    private val owner: Closeable,
) : InputStream() {
    override fun read(): Int {
        if (limit <= 0) return -1
        return source.read().also { if (it >= 0) limit-- }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (limit <= 0) return -1
        val read = source.read(buffer, offset, minOf(length.toLong(), limit).toInt())
        if (read > 0) limit -= read
        return read
    }

    override fun close() {
        source.close()
        owner.close()
    }
}
