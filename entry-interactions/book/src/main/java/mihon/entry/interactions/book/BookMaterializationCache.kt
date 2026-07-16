package mihon.entry.interactions.book

import android.app.Application
import android.text.format.Formatter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mihon.book.api.BookContentResource
import mihon.book.api.BookResourceCacheState
import mihon.entry.interactions.EntryMediaCacheBucket
import mihon.entry.interactions.EntryMediaCacheBucketKeys
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID

internal data class BookMaterializationKey(
    val publicationId: String,
    val resourceId: String,
    val revision: String,
    val mediaType: String?,
) {
    fun diskKey(): String = MessageDigest.getInstance("SHA-256")
        .digest(
            listOf(publicationId, resourceId, revision, mediaType.orEmpty())
                .joinToString(separator = "\u0000")
                .encodeToByteArray(),
        )
        .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }
}

internal interface BookMaterializationStore {
    suspend fun acquire(
        key: BookMaterializationKey?,
        metadata: BookContentResource,
        write: suspend (File) -> Unit,
    ): MaterializedBookResource

    fun cacheState(key: BookMaterializationKey?): BookResourceCacheState
}

internal class BookMaterializationCache(
    private val application: Application,
    private val directory: File = application.cacheDir.resolve(CACHE_DIRECTORY_NAME),
    private val maxCacheBytes: Long = MAX_CACHE_BYTES,
    private val maxResourceBytes: Long = MAX_RESOURCE_BYTES,
) : BookMaterializationStore, EntryMediaCacheBucket {
    private val stateLock = Any()
    private val activeLeaseCounts = mutableMapOf<File, Int>()
    private val activeWrites = mutableSetOf<File>()
    private val invalidatedFiles = mutableSetOf<File>()
    private val keyLocks = mutableMapOf<String, ReferencedMutex>()
    private var initialized = false

    override val key: String = EntryMediaCacheBucketKeys.BOOK_MATERIALIZED

    override val readableSize: String
        get() = Formatter.formatFileSize(application, directory.cacheFiles().sumOf(File::length))

    override suspend fun acquire(
        key: BookMaterializationKey?,
        metadata: BookContentResource,
        write: suspend (File) -> Unit,
    ): MaterializedBookResource {
        ensureInitialized()
        return if (key == null) {
            createTransient(metadata, write)
        } else {
            val diskKey = key.diskKey()
            withKeyLock(diskKey) {
                val target = directory.resolve(diskKey + metadata.fileSuffix())
                if (!target.isUsableCacheFile()) {
                    target.delete()
                    writeAtomically(target, metadata, write)
                }
                target.setLastModified(System.currentTimeMillis())
                createLease(target, metadata.cached(), deleteOnClose = false).also {
                    prune()
                }
            }
        }
    }

    override fun cacheState(key: BookMaterializationKey?): BookResourceCacheState {
        if (key == null || !directory.isDirectory) return BookResourceCacheState.UNKNOWN
        val prefix = key.diskKey()
        return if (directory.listFiles().orEmpty().any { it.name.startsWith(prefix) && it.isUsableCacheFile() }) {
            BookResourceCacheState.CACHED
        } else {
            BookResourceCacheState.UNKNOWN
        }
    }

    override fun clear(): Int = synchronized(stateLock) {
        ensureDirectory()
        directory.listFiles().orEmpty().count { file ->
            file !in activeLeaseCounts && file !in activeWrites && file.deleteRecursively()
        }
    }

    private suspend fun createTransient(
        metadata: BookContentResource,
        write: suspend (File) -> Unit,
    ): MaterializedBookResource {
        val file = createPartFile(metadata)
        try {
            write(file)
            validateWrittenFile(file)
            return createLease(file, metadata, deleteOnClose = true)
        } catch (error: Throwable) {
            file.delete()
            throw error
        } finally {
            synchronized(stateLock) { activeWrites.remove(file) }
        }
    }

    private suspend fun writeAtomically(
        target: File,
        metadata: BookContentResource,
        write: suspend (File) -> Unit,
    ) {
        val part = createPartFile(metadata)
        try {
            write(part)
            validateWrittenFile(part)
            synchronized(stateLock) { activeWrites += target }
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
            synchronized(stateLock) { invalidatedFiles.remove(target) }
        } catch (error: Throwable) {
            part.delete()
            target.delete()
            synchronized(stateLock) { activeWrites.remove(target) }
            throw error
        } finally {
            synchronized(stateLock) { activeWrites.remove(part) }
        }
    }

    private fun validateWrittenFile(file: File) {
        check(file.isFile) { "BOOK materialization did not produce a file" }
        require(file.length() <= maxResourceBytes) { "BOOK resource exceeds the materialization limit" }
    }

    private fun createPartFile(metadata: BookContentResource): File {
        ensureDirectory()
        return directory.resolve(".${UUID.randomUUID()}${metadata.fileSuffix()}.part").also { file ->
            synchronized(stateLock) { activeWrites += file }
        }
    }

    private fun createLease(
        file: File,
        metadata: BookContentResource,
        deleteOnClose: Boolean,
    ): MaterializedBookResource {
        synchronized(stateLock) {
            activeLeaseCounts[file] = activeLeaseCounts.getOrDefault(file, 0) + 1
            activeWrites.remove(file)
        }
        return CachedMaterializedBookResource(
            metadata = metadata,
            file = file,
            onInvalidate = { invalidate(file) },
            onClose = {
                val released = synchronized(stateLock) {
                    val remaining = activeLeaseCounts.getOrDefault(file, 1) - 1
                    if (remaining <= 0) activeLeaseCounts.remove(file) else activeLeaseCounts[file] = remaining
                    if (deleteOnClose && remaining <= 0) file.delete()
                    remaining <= 0
                }
                if (released && !deleteOnClose) prune()
            },
        )
    }

    private fun prune(): Unit = synchronized(stateLock) {
        val files = directory.cacheFiles().sortedBy(File::lastModified)
        var total = files.sumOf(File::length)
        files.forEach { file ->
            if (total <= maxCacheBytes) return
            if (file !in activeLeaseCounts) {
                val length = file.length()
                if (file.delete()) total -= length
            }
        }
    }

    private fun invalidate(file: File) = synchronized(stateLock) {
        invalidatedFiles += file
        if (file.delete()) invalidatedFiles.remove(file)
    }

    private fun ensureInitialized(): Unit = synchronized(stateLock) {
        if (initialized) return
        ensureDirectory()
        directory.listFiles().orEmpty()
            .filter { it.name.endsWith(PART_SUFFIX) || !it.name.matches(CACHE_FILE_PATTERN) }
            .forEach(File::deleteRecursively)
        initialized = true
    }

    private fun ensureDirectory() {
        check(directory.mkdirs() || directory.isDirectory) {
            "Unable to create BOOK materialization cache directory"
        }
    }

    private suspend fun <T> withKeyLock(key: String, block: suspend () -> T): T {
        val referenced = synchronized(stateLock) {
            keyLocks.getOrPut(key, ::ReferencedMutex).also { it.references++ }
        }
        return try {
            referenced.mutex.withLock { block() }
        } finally {
            synchronized(stateLock) {
                referenced.references--
                if (referenced.references == 0) keyLocks.remove(key, referenced)
            }
        }
    }

    private fun File.isUsableCacheFile(): Boolean = synchronized(stateLock) {
        this !in invalidatedFiles && isFile && length() in 1..maxResourceBytes
    }

    private fun File.cacheFiles(): List<File> = listFiles().orEmpty()
        .filter { it.name.matches(CACHE_FILE_PATTERN) && it.isUsableCacheFile() }

    private companion object {
        const val CACHE_DIRECTORY_NAME = "book_materialized"
        const val PART_SUFFIX = ".part"
        const val MAX_CACHE_BYTES = 1024L * 1024L * 1024L
        const val MAX_RESOURCE_BYTES = 512L * 1024L * 1024L
        val CACHE_FILE_PATTERN = Regex("[a-f0-9]{64}\\.[A-Za-z0-9]+")
    }
}

private class ReferencedMutex(
    val mutex: Mutex = Mutex(),
    var references: Int = 0,
)

private class CachedMaterializedBookResource(
    override val metadata: BookContentResource,
    override val file: File,
    private val onInvalidate: () -> Unit,
    private val onClose: () -> Unit,
) : MaterializedBookResource {
    private var closed = false
    private var invalidated = false

    override fun invalidate() {
        synchronized(this) {
            if (invalidated) return
            invalidated = true
        }
        onInvalidate()
    }

    override fun close() {
        synchronized(this) {
            if (closed) return
            closed = true
        }
        onClose()
    }
}

private fun BookContentResource.cached(): BookContentResource = copy(cacheState = BookResourceCacheState.CACHED)

internal fun BookContentResource.fileSuffix(): String = when (mediaType) {
    "application/epub+zip" -> ".epub"
    "text/html", "application/xhtml+xml" -> ".html"
    "text/plain" -> ".txt"
    else -> ".bin"
}
