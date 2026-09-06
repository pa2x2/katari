package mihon.entry.interactions.book.format.epub.archive

import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Materializes the authored resources of a small publication without altering XML namespaces. */
internal fun epubArchiveFile(resources: Map<String, String>): File {
    val file = Files.createTempFile("epub-archive", ".epub").toFile()
    ZipOutputStream(file.outputStream()).use { zip ->
        resources.forEach { (path, value) ->
            zip.putNextEntry(ZipEntry(path))
            zip.write(value.encodeToByteArray())
            zip.closeEntry()
        }
    }
    return file
}
