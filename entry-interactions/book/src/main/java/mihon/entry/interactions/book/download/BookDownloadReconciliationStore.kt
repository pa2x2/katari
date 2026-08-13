package mihon.entry.interactions.book.download

import android.content.Context
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.File

/** Independent app-private summary for filesystem-authoritative metadata reconciliation. */
@OptIn(ExperimentalSerializationApi::class)
internal class BookDownloadReconciliationStore(
    private val file: File,
) {
    constructor(context: Context) : this(
        File(context.durableBookDownloadReconciliationDirectory(), FILE_NAME),
    )

    @Synchronized
    fun read(downloadsRootUri: String): Map<String, BookDownloadReconciliationSource> {
        val atomicFile = BookDownloadAtomicFile(file)
        if (!atomicFile.exists()) return emptyMap()
        return runCatching {
            val snapshot = atomicFile.openRead().use { input ->
                ProtoBuf.decodeFromByteArray<BookDownloadReconciliationSnapshot>(input.readBytes())
            }
            require(snapshot.version == CURRENT_VERSION)
            require(snapshot.downloadsRootUri == downloadsRootUri)
            snapshot.sources.associateBy(BookDownloadReconciliationSource::directoryUri)
        }.getOrElse {
            atomicFile.delete()
            emptyMap()
        }
    }

    @Synchronized
    fun replace(
        downloadsRootUri: String,
        sources: Collection<BookDownloadReconciliationSource>,
    ) {
        val snapshot = BookDownloadReconciliationSnapshot(
            downloadsRootUri = downloadsRootUri,
            sources = sources.sortedBy(BookDownloadReconciliationSource::directoryUri),
        )
        val bytes = ProtoBuf.encodeToByteArray(snapshot)
        if (bytes.size > MAX_SNAPSHOT_BYTES) return
        runCatching { BookDownloadAtomicFile(file).write(bytes) }
    }

    private companion object {
        const val FILE_NAME = "book_download_reconciliation_v2"
        const val CURRENT_VERSION = 2
        const val MAX_SNAPSHOT_BYTES = 32 * 1024 * 1024
    }
}

@Serializable
internal data class BookDownloadReconciliationSource(
    val directoryUri: String,
    val entries: List<BookDownloadReconciliationEntry>,
)

@Serializable
internal data class BookDownloadReconciliationEntry(
    val directoryName: String,
    /** Retained in the version 2 snapshot format, but not trusted as a package reuse token. */
    val directoryLastModified: Long,
    val packages: List<BookDownloadReconciliationPackage>,
)

@Serializable
internal data class BookDownloadReconciliationPackage(
    val directoryName: String,
    val directoryLastModified: Long,
    val download: IndexedBookDownloadPackage,
) {
    fun matches(directory: BookDownloadDirectoryEntry): Boolean =
        directoryLastModified > 0L &&
            directory.lastModified > 0L &&
            directoryLastModified == directory.lastModified
}

@Serializable
private data class BookDownloadReconciliationSnapshot(
    val version: Int = 2,
    val downloadsRootUri: String,
    val sources: List<BookDownloadReconciliationSource>,
)

private fun Context.durableBookDownloadReconciliationDirectory(): File =
    runCatching { noBackupFilesDir }.getOrNull()
        ?.takeIf { directory -> runCatching { directory.path.isNotBlank() }.getOrDefault(false) }
        ?: filesDir
