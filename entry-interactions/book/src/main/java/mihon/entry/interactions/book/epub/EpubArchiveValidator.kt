package mihon.entry.interactions.book.epub

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import mihon.book.api.BookFailure
import mihon.book.api.BookFailureReason
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile

internal data class EpubArchiveLimits(
    val maxCompressedBytes: Long = 512L * 1024L * 1024L,
    val maxUncompressedBytes: Long = 1024L * 1024L * 1024L,
    val maxEntryBytes: Long = 128L * 1024L * 1024L,
    val maxEntries: Int = 10_000,
    val maxCompressionRatio: Long = 100L,
    val compressionRatioMinimumBytes: Long = 1024L * 1024L,
    val maxEntryNameLength: Int = 1024,
)

internal class EpubArchiveValidator(
    private val limits: EpubArchiveLimits = EpubArchiveLimits(),
) {
    suspend fun validate(file: File): BookFailure? = withContext(Dispatchers.IO) {
        try {
            validateArchive(file)
            null
        } catch (error: EpubArchiveException) {
            BookFailure(error.reason, error.message ?: "Invalid EPUB archive")
        } catch (error: ZipException) {
            BookFailure(BookFailureReason.MALFORMED_CONTENT, error.message ?: "Invalid EPUB ZIP archive")
        } catch (error: IOException) {
            BookFailure(BookFailureReason.MALFORMED_CONTENT, error.message ?: "EPUB archive cannot be read")
        } catch (error: SecurityException) {
            BookFailure(BookFailureReason.MALFORMED_CONTENT, error.message ?: "EPUB archive cannot be inspected")
        }
    }

    private suspend fun validateArchive(file: File) {
        malformed(file.isFile && file.length() > 0L, "EPUB resource is empty")
        malformed(file.length() <= limits.maxCompressedBytes, "EPUB archive exceeds the compressed-size limit")

        ZipFile(file).use { archive ->
            var entryCount = 0
            var declaredUncompressed = 0L
            var actualUncompressed = 0L
            val names = mutableSetOf<String>()
            var mimetypeEntry: ZipEntry? = null
            var hasContainer = false
            val entries = archive.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                entryCount++
                malformed(entryCount <= limits.maxEntries, "EPUB archive contains too many entries")
                validateName(entry.name)
                malformed(names.add(entry.name), "EPUB archive contains duplicate entry names")
                if (entry.name == MIMETYPE_PATH) mimetypeEntry = entry
                if (entry.name == CONTAINER_PATH) hasContainer = true
                if (entry.isDirectory) continue

                val size = entry.size
                val compressedSize = entry.compressedSize
                malformed(size >= 0L && compressedSize >= 0L, "EPUB archive contains an entry with unknown size")
                malformed(size <= limits.maxEntryBytes, "EPUB archive contains an oversized entry")
                malformed(
                    declaredUncompressed <= limits.maxUncompressedBytes - size,
                    "EPUB archive exceeds the expanded-size limit",
                )
                declaredUncompressed += size

                val actualSize = archive.getInputStream(entry).use { input ->
                    var entryBytes = 0L
                    val buffer = ByteArray(VALIDATION_BUFFER_SIZE)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        entryBytes += read
                        malformed(entryBytes <= limits.maxEntryBytes, "EPUB archive contains an oversized entry")
                        malformed(
                            actualUncompressed <= limits.maxUncompressedBytes - read,
                            "EPUB archive exceeds the expanded-size limit",
                        )
                        actualUncompressed += read
                    }
                    entryBytes
                }
                malformed(actualSize == size, "EPUB archive entry size does not match its directory record")
                if (actualSize >= limits.compressionRatioMinimumBytes) {
                    malformed(
                        compressedSize > 0L && actualSize / compressedSize <= limits.maxCompressionRatio,
                        "EPUB archive contains a suspiciously compressed entry",
                    )
                }
            }

            malformed(entryCount > 0, "EPUB archive is empty")
            val mimetype = mimetypeEntry ?: malformed("EPUB archive is missing its mimetype entry")
            malformed(hasContainer, "EPUB archive is missing META-INF/container.xml")
            malformed(!mimetype.isDirectory, "EPUB mimetype entry is not a file")
            val mediaType = archive.getInputStream(mimetype).use(::readMimetype)
            malformed(mediaType.trim() == EPUB_MEDIA_TYPE, "EPUB mimetype entry is invalid")
        }
    }

    private fun validateName(name: String) {
        malformed(name.isNotBlank(), "EPUB archive contains an empty entry name")
        malformed(name.length <= limits.maxEntryNameLength, "EPUB archive contains an overlong entry name")
        malformed('\u0000' !in name, "EPUB archive contains a null byte in an entry name")
        malformed(!name.startsWith('/') && !name.startsWith('\\'), "EPUB archive contains an absolute path")
        malformed(!DRIVE_PATH.matches(name), "EPUB archive contains a drive-qualified path")
        malformed('\\' !in name, "EPUB archive contains a non-portable path separator")
        malformed(name.split('/').none { it == ".." }, "EPUB archive contains a parent-directory path")
    }

    private fun readMimetype(input: java.io.InputStream): String {
        val buffer = ByteArray(MAX_MIMETYPE_BYTES + 1)
        var read = 0
        while (read < buffer.size) {
            val count = input.read(buffer, read, buffer.size - read)
            if (count < 0) break
            read += count
        }
        malformed(read <= MAX_MIMETYPE_BYTES, "EPUB mimetype entry is oversized")
        return buffer.decodeToString(endIndex = read)
    }

    private fun malformed(condition: Boolean, message: String) {
        if (!condition) throw EpubArchiveException(BookFailureReason.MALFORMED_CONTENT, message)
    }

    private fun malformed(message: String): Nothing {
        throw EpubArchiveException(BookFailureReason.MALFORMED_CONTENT, message)
    }

    private companion object {
        const val EPUB_MEDIA_TYPE = "application/epub+zip"
        const val MIMETYPE_PATH = "mimetype"
        const val CONTAINER_PATH = "META-INF/container.xml"
        const val MAX_MIMETYPE_BYTES = 64
        const val VALIDATION_BUFFER_SIZE = 32 * 1024
        val DRIVE_PATH = Regex("^[A-Za-z]:.*")
    }
}

private class EpubArchiveException(
    val reason: BookFailureReason,
    message: String,
) : IllegalArgumentException(message)
