package mihon.entry.interactions.book.format.epub.archive

import java.io.File
import java.nio.file.Files
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Materializes the authored resources of a small publication without altering XML namespaces. */
internal fun epubArchiveFile(resources: Map<String, String>): File =
    epubArchiveBytesFile(resources.mapValues { (_, text) -> text.encodeToByteArray() })

internal fun epubArchiveBytesFile(resources: Map<String, ByteArray>): File {
    val file = Files.createTempFile("epub-archive", ".epub").toFile()
    ZipOutputStream(file.outputStream()).use { zip ->
        resources.forEach { (path, value) ->
            val entry = ZipEntry(path)
            if (path == "mimetype") {
                entry.method = ZipEntry.STORED
                entry.size = value.size.toLong()
                entry.crc = CRC32().apply { update(value) }.value
            }
            zip.putNextEntry(entry)
            zip.write(value)
            zip.closeEntry()
        }
    }
    return file
}
