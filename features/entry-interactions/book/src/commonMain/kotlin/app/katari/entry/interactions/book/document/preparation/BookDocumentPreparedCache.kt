package app.katari.entry.interactions.book.document.preparation

import app.katari.core.platform.storage.AppPrivateDirectories
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

/** Persistent bounded cache for exact-revision canonical document publications. */
internal class BookDocumentPreparedCache(
    private val directory: File,
    private val maxBytes: Long = MAX_CACHE_BYTES,
    private val maxEntryBytes: Long = MAX_ENTRY_BYTES,
) {
    constructor(directories: AppPrivateDirectories) : this(directories.noBackupFiles.resolve(CACHE_DIRECTORY_NAME))

    /** [onProgress] gets the share of the entry read so far, 0..1; a large book's entry takes seconds to decode. */
    @OptIn(ExperimentalSerializationApi::class)
    @Synchronized
    fun read(
        key: BookDocumentPreparedCacheKey,
        onProgress: (Float) -> Unit = {},
    ): BookDocumentPreparedCacheValue? {
        val file = directory.resolve("${key.diskKey()}.json")
        return runCatching {
            ensureDirectory()
            val length = file.length()
            if (!file.isFile || length !in 1..maxEntryBytes) return null
            ReadProgressInputStream(file.inputStream().buffered(), length, onProgress)
                .use { input -> JSON.decodeFromStream<BookDocumentPreparedCacheEntry>(input) }
                .takeIf { entry -> entry.schemaVersion == SCHEMA_VERSION && entry.key == key }
                ?.value
                ?.also { file.setLastModified(System.currentTimeMillis()) }
        }.getOrElse {
            runCatching { file.delete() }
            null
        }
    }

    @Synchronized
    fun write(key: BookDocumentPreparedCacheKey, value: BookDocumentPreparedCacheValue): Boolean {
        val target = directory.resolve("${key.diskKey()}.json")
        val part = directory.resolve(".${UUID.randomUUID()}.part")
        return try {
            ensureDirectory()
            part.writeText(
                JSON.encodeToString(
                    BookDocumentPreparedCacheEntry(
                        schemaVersion = SCHEMA_VERSION,
                        key = key,
                        value = value,
                    ),
                ),
            )
            if (part.length() !in 1..minOf(maxEntryBytes, maxBytes)) return false
            try {
                Files.move(
                    part.toPath(),
                    target.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(part.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            target.setLastModified(System.currentTimeMillis())
            prune()
            true
        } catch (_: IOException) {
            false
        } catch (_: SecurityException) {
            false
        } finally {
            runCatching { part.delete() }
        }
    }

    @Synchronized
    fun removePublication(publicationId: String) {
        if (!directory.isDirectory) return
        directory.cacheFiles().forEach { file ->
            val matches = runCatching {
                JSON.decodeFromString<BookDocumentPreparedCacheEntry>(file.readText()).key.publicationId ==
                    publicationId
            }.getOrDefault(true)
            if (matches) file.delete()
        }
    }

    private fun prune() {
        val files = directory.cacheFiles().sortedBy(File::lastModified)
        var total = files.sumOf(File::length)
        files.forEach { file ->
            if (total <= maxBytes) return
            val length = file.length()
            if (file.delete()) total -= length
        }
    }

    private fun ensureDirectory() {
        if (!directory.mkdirs() && !directory.isDirectory) {
            throw IOException("Unable to create prepared document cache directory")
        }
        directory.listFiles().orEmpty()
            .filter { file -> file.name.endsWith(".part") || !file.name.matches(CACHE_FILE_PATTERN) }
            .forEach(File::delete)
    }

    private fun File.cacheFiles(): List<File> = listFiles().orEmpty()
        .filter { file -> file.isFile && file.name.matches(CACHE_FILE_PATTERN) && file.length() in 1..maxEntryBytes }

    private companion object {
        const val CACHE_DIRECTORY_NAME = "book_prepared_documents_v1"

        // Also invalidates prepared semantics when parsing or resource projection changes.
        const val SCHEMA_VERSION = 12
        const val MAX_CACHE_BYTES = 64L * 1024L * 1024L
        const val MAX_ENTRY_BYTES = 64L * 1024L * 1024L
        val CACHE_FILE_PATTERN = Regex("[a-f0-9]{64}\\.json")
        val JSON = Json {
            encodeDefaults = true
            ignoreUnknownKeys = false
        }
    }
}
