package mihon.entry.interactions.book.format.epub.archive

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/** Path-confined access to one materialized publication container. */
internal class EpubArchive(file: File) : AutoCloseable {
    private val zip = ZipFile(file)
    private val entries: Map<String, ZipEntry> = buildMap {
        zip.entries().asSequence().filterNot(ZipEntry::isDirectory).forEach { entry ->
            val path = normalizeArchivePath(entry.name)
            require(put(path, entry) == null) { "Publication archive contains duplicate path $path" }
        }
    }

    fun contains(path: String): Boolean = normalizeArchivePath(path) in entries

    fun read(path: String, maxBytes: Int): ByteArray {
        val normalized = normalizeArchivePath(path)
        val entry = requireNotNull(entries[normalized]) { "Publication resource is missing: $normalized" }
        require(entry.size < 0L || entry.size <= maxBytes) { "Publication resource exceeds its byte limit" }
        return zip.getInputStream(entry).use { input ->
            val output = java.io.ByteArrayOutputStream(minOf(maxBytes, DEFAULT_BUFFER_SIZE))
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                require(total <= maxBytes) { "Publication resource exceeds its byte limit" }
                output.write(buffer, 0, read)
            }
            output.toByteArray()
        }
    }

    override fun close() = zip.close()
}
