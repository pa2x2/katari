package mihon.entry.interactions.book.document.preparation

import android.app.Application
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mihon.book.api.document.BookDocumentPublicationModel
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID

/** Persistent bounded cache for exact-revision canonical document publications. */
internal class BookDocumentPreparedCache(
    application: Application,
    private val directory: File = application.noBackupFilesDir.resolve(CACHE_DIRECTORY_NAME),
    private val maxBytes: Long = MAX_CACHE_BYTES,
) {
    @Synchronized
    fun read(key: BookDocumentPreparedCacheKey): BookDocumentPreparedCacheValue? {
        ensureDirectory()
        val file = directory.resolve("${key.diskKey()}.json")
        if (!file.isFile || file.length() !in 1..MAX_ENTRY_BYTES) return null
        return runCatching {
            JSON.decodeFromString<BookDocumentPreparedCacheEntry>(file.readText())
                .takeIf { entry -> entry.schemaVersion == SCHEMA_VERSION && entry.key == key }
                ?.value
                ?.also { file.setLastModified(System.currentTimeMillis()) }
        }.getOrElse {
            file.delete()
            null
        }
    }

    @Synchronized
    fun write(key: BookDocumentPreparedCacheKey, value: BookDocumentPreparedCacheValue) {
        ensureDirectory()
        val target = directory.resolve("${key.diskKey()}.json")
        val part = directory.resolve(".${UUID.randomUUID()}.part")
        try {
            part.writeText(
                JSON.encodeToString(
                    BookDocumentPreparedCacheEntry(
                        schemaVersion = SCHEMA_VERSION,
                        key = key,
                        value = value,
                    ),
                ),
            )
            require(part.length() in 1..MAX_ENTRY_BYTES) { "Prepared document cache entry is too large" }
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
        } finally {
            part.delete()
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
        check(directory.mkdirs() || directory.isDirectory) { "Unable to create prepared document cache directory" }
        directory.listFiles().orEmpty()
            .filter { file -> file.name.endsWith(".part") || !file.name.matches(CACHE_FILE_PATTERN) }
            .forEach(File::delete)
    }

    private fun File.cacheFiles(): List<File> = listFiles().orEmpty()
        .filter { file -> file.isFile && file.name.matches(CACHE_FILE_PATTERN) && file.length() in 1..MAX_ENTRY_BYTES }

    private companion object {
        const val CACHE_DIRECTORY_NAME = "book_prepared_documents_v1"
        const val SCHEMA_VERSION = 3
        const val MAX_CACHE_BYTES = 64L * 1024L * 1024L
        const val MAX_ENTRY_BYTES = 16L * 1024L * 1024L
        val CACHE_FILE_PATTERN = Regex("[a-f0-9]{64}\\.json")
        val JSON = Json {
            encodeDefaults = true
            ignoreUnknownKeys = false
        }
    }
}

@Serializable
internal data class BookDocumentPreparedCacheKey(
    val publicationId: String,
    val revision: String,
    val modelId: String = BookDocumentPublicationModel.DESCRIPTOR.id,
    val modelVersion: Int = BookDocumentPublicationModel.DESCRIPTOR.version,
) {
    init {
        require(publicationId.isNotBlank()) { "prepared document publication id must not be blank" }
        require(revision.isNotBlank()) { "prepared document revision must not be blank" }
    }

    fun diskKey(): String = MessageDigest.getInstance("SHA-256")
        .digest(listOf(publicationId, revision, modelId, modelVersion).joinToString("\u0000").encodeToByteArray())
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
}

@Serializable
internal data class BookDocumentPreparedCacheValue(
    val model: BookDocumentPublicationModel,
    val documentTitles: Map<String, String?>,
    val derivedResources: List<BookDocumentCachedResource> = emptyList(),
    val remoteResources: List<BookDocumentCachedRemoteResource> = emptyList(),
)

@Serializable
internal data class BookDocumentCachedResource(
    val resourceId: String,
    val mediaType: String,
    val bytes: ByteArray,
)

@Serializable
internal data class BookDocumentCachedRemoteResource(
    val resourceId: String,
    val url: String,
    val type: String,
)

@Serializable
private data class BookDocumentPreparedCacheEntry(
    val schemaVersion: Int,
    val key: BookDocumentPreparedCacheKey,
    val value: BookDocumentPreparedCacheValue,
)
