package app.katari.video.engine.mpv.proxy

import java.io.FileNotFoundException
import java.io.InputStream
import java.net.URI
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/** Downloaded files on the desktop, which are plain paths (`file://`). */
internal class DesktopLocalMediaFiles : LocalMediaFiles {
    override fun name(uri: String): String = path(uri).fileName?.toString().orEmpty()

    override fun sibling(uri: String, name: String): String? = path(uri).resolveSibling(name).toUri().toString()

    override fun readText(uri: String): String = Files.readString(existing(uri))

    override fun open(uri: String, range: ByteRange?): OpenedLocalMedia {
        val channel = FileChannel.open(existing(uri), StandardOpenOption.READ)
        val size = channel.size()
        val start = range?.first?.coerceAtMost(size) ?: 0L
        val end = range?.last?.coerceAtMost(size - 1) ?: (size - 1)
        val length = (end - start + 1).coerceAtLeast(0)
        channel.position(start)
        return OpenedLocalMedia(
            stream = BoundedInputStream(Channels.newInputStream(channel), length),
            length = length,
            contentRange = range?.let { "bytes $start-$end/$size" },
        )
    }

    private fun existing(uri: String): Path = path(uri).also {
        if (!Files.isRegularFile(it)) throw FileNotFoundException(uri)
    }

    private fun path(uri: String): Path {
        val parsed = URI(uri)
        require(parsed.scheme == "file") { "Not a file: $uri" }
        return Path.of(parsed)
    }
}

/** Reads at most [limit] bytes of [source]. */
private class BoundedInputStream(private val source: InputStream, private var limit: Long) : InputStream() {
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

    override fun close() = source.close()
}
