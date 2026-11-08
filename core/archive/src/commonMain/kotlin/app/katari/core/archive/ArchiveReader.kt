package app.katari.core.archive

import java.io.Closeable
import java.io.InputStream

/**
 * Reads a zip, RAR (RAR5 included), 7z or tar archive, and the epubs and comic book archives (cbz, cbr, cb7, cbt)
 * that are one of those, through libarchive. Each read goes through the archive from its start.
 */
expect class ArchiveReader : Closeable {
    fun <T> useEntries(block: (Sequence<ArchiveEntry>) -> T): T

    /** Positioned at the start of [entryName]'s data; null when the archive has no such entry. */
    fun getInputStream(entryName: String): InputStream?

    override fun close()
}
