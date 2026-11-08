package app.katari.video.engine.mpv.proxy

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.net.Uri
import androidx.core.net.toFile
import androidx.core.net.toUri
import app.katari.video.engine.spi.loading.siblingDocumentUri
import java.io.Closeable
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream

/** Downloaded files on Android: documents of the storage tree (`content://`) and plain paths (`file://`). */
internal class AndroidLocalMediaFiles(private val context: Context) : LocalMediaFiles {

    override fun name(uri: String): String = Uri.parse(uri).lastPathSegment.orEmpty().substringAfterLast('/')

    override fun sibling(uri: String, name: String): String? {
        val file = Uri.parse(uri)
        return when (file.scheme) {
            "file" -> file.toFile().resolveSibling(name).toUri()
            else -> siblingDocumentUri(file, name)
        }?.toString()
    }

    override fun readText(uri: String): String {
        val file = Uri.parse(uri)
        return when (file.scheme) {
            "file" -> file.toFile().readText()
            else -> context.contentResolver.openInputStream(file)?.use { it.bufferedReader().readText() }
                ?: throw FileNotFoundException(uri)
        }
    }

    override fun open(uri: String, range: ByteRange?): OpenedLocalMedia {
        val descriptor = context.contentResolver.openAssetFileDescriptor(Uri.parse(uri), "r")
            ?: throw FileNotFoundException(uri)
        val size = descriptor.length.takeIf { it != AssetFileDescriptor.UNKNOWN_LENGTH }
            ?: descriptor.parcelFileDescriptor.statSize
        val stream = FileInputStream(descriptor.fileDescriptor)
        val start = range?.first?.coerceAtMost(size) ?: 0L
        val end = range?.last?.coerceAtMost(size - 1) ?: (size - 1)
        stream.channel.position(descriptor.startOffset + start)
        return OpenedLocalMedia(
            stream = BoundedInputStream(stream, (end - start + 1).coerceAtLeast(0), descriptor),
            length = (end - start + 1).coerceAtLeast(0),
            contentRange = range?.let { "bytes $start-$end/$size" },
        )
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
