package app.katari.core.archive

import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path

/** Fails with [LibarchiveUnavailableException] where the app carries no libarchive and the system has none. */
actual class ArchiveReader(private val path: Path) : Closeable {
    private val library = libarchive()

    actual fun <T> useEntries(block: (Sequence<ArchiveEntry>) -> T): T = LibarchiveInputStream(library, path).use {
        block(generateSequence { it.getNextEntry() })
    }

    actual fun getInputStream(entryName: String): InputStream? {
        val archive = LibarchiveInputStream(library, path)
        try {
            while (true) {
                val entry = archive.getNextEntry() ?: break
                if (entry.name == entryName) {
                    return archive
                }
            }
        } catch (e: IOException) {
            archive.close()
            throw e
        }
        archive.close()
        return null
    }

    // Each read opens the file itself, so nothing stays open between reads.
    actual override fun close() = Unit
}
