package eu.kanade.tachiyomi.data.backup.storage

import okio.Buffer
import okio.ForwardingSource
import okio.Source

/**
 * How far reading a backup file has got, for a screen that waits on it: [fraction] of the file's [fileSize] bytes,
 * counting every read the work makes of it, and the [entries] it has handed on so far.
 */
data class BackupReadProgress(val fraction: Float, val fileSize: Long, val entries: Int)

/**
 * Counts the bytes of [reads] reads of a file of [fileSize] bytes, and the entries handed on, into
 * [BackupReadProgress]. Reports at most every [INTERVAL_NANOS], as a read hands bytes over a few kilobytes at a time;
 * reports nothing when the file's storage doesn't say its size.
 */
internal class BackupReadCounter(
    private val fileSize: Long,
    private val reads: Int,
    private val report: (BackupReadProgress) -> Unit,
) {
    private var bytes = 0L
    private var entries = 0
    private var reportedAt = 0L

    /** [source], with what is read from it counted. */
    fun counted(source: Source): Source = object : ForwardingSource(source) {
        override fun read(sink: Buffer, byteCount: Long): Long {
            val read = super.read(sink, byteCount)
            if (read > 0) {
                bytes += read
                maybeReport()
            }
            return read
        }
    }

    fun countEntry() {
        entries++
        maybeReport()
    }

    private fun maybeReport() {
        if (fileSize <= 0) return
        val now = System.nanoTime()
        if (now - reportedAt < INTERVAL_NANOS) return
        reportedAt = now
        val fraction = (bytes.toDouble() / (fileSize.toDouble() * reads)).coerceIn(0.0, 1.0).toFloat()
        report(BackupReadProgress(fraction, fileSize, entries))
    }

    private companion object {
        const val INTERVAL_NANOS = 100_000_000L
    }
}
