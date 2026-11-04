package mihon.core.archive

import tachiyomi.core.common.storage.StorageFile
import java.io.Closeable
import java.io.InputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Writes a zip of uncompressed entries into [file], replacing what it held: a comic book archive's pages, which are
 * images that compress no further. A stored entry needs its size and checksum ahead of its data, so each file is read
 * twice, which keeps a long strip's page out of memory.
 */
class ZipWriter(file: StorageFile) : Closeable {
    private val output = ZipOutputStream(file.openOutputStream().buffered())

    fun write(file: StorageFile) {
        val crc = CRC32()
        val size = file.openInputStream().use { input -> input.forEachBlock(crc::update) }
        val entry = ZipEntry(checkNotNull(file.name) { "A page without a name: ${file.uri}" }).apply {
            method = ZipEntry.STORED
            this.size = size
            compressedSize = size
            this.crc = crc.value
            time = file.lastModified()
        }
        output.putNextEntry(entry)
        file.openInputStream().use { input -> input.copyTo(output) }
        output.closeEntry()
    }

    override fun close() {
        output.close()
    }
}

private inline fun InputStream.forEachBlock(action: (ByteArray, Int, Int) -> Unit): Long {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val read = read(buffer)
        if (read < 0) return total
        action(buffer, 0, read)
        total += read
    }
}
