package app.katari.entry.interactions.book.document.preparation

import java.io.FilterInputStream
import java.io.InputStream

/** Reports the share of [totalBytes] read through it, at most once per percent so a reader's UI isn't flooded. */
internal class ReadProgressInputStream(
    delegate: InputStream,
    private val totalBytes: Long,
    private val onProgress: (Float) -> Unit,
) : FilterInputStream(delegate) {
    private var readBytes = 0L
    private var reportedPercent = -1

    override fun read(): Int = super.read().also { if (it >= 0) advance(1L) }

    override fun read(b: ByteArray, off: Int, len: Int): Int =
        super.read(b, off, len).also { if (it > 0) advance(it.toLong()) }

    override fun skip(n: Long): Long = super.skip(n).also(::advance)

    private fun advance(bytes: Long) {
        readBytes += bytes
        val percent = (readBytes * 100 / totalBytes).toInt().coerceIn(0, 100)
        if (percent == reportedPercent) return
        reportedPercent = percent
        onProgress(percent / 100f)
    }
}
