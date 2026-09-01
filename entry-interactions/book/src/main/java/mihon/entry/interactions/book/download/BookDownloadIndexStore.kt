package mihon.entry.interactions.book.download

import android.content.Context
import androidx.core.net.toUri
import com.hippo.unifile.UniFile
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.File

/** Durable metadata index for complete BOOK packages. Download packages remain the source of truth. */
@OptIn(ExperimentalSerializationApi::class)
internal class BookDownloadIndexStore private constructor(
    private val context: Context,
    private val indexFile: File,
    private val legacyCacheFile: File?,
) {
    private val reconciliationMarker = File(indexFile.path + RECONCILIATION_SUFFIX)
    var lastReadRequiresVerification: Boolean = false
        private set
    private var lastReadRequiresReconciliation: Boolean = false

    constructor(context: Context) : this(
        context = context,
        indexFile = File(context.durableBookDownloadDirectory(), INDEX_FILE_NAME),
        legacyCacheFile = File(context.cacheDir, LEGACY_CACHE_FILE_NAME),
    )

    /** Test and compatibility seam for an explicitly located index. */
    constructor(context: Context, cacheFile: File) : this(
        context = context,
        indexFile = cacheFile,
        legacyCacheFile = null,
    )

    private val journal = BookDownloadIndexJournal(File(indexFile.path + JOURNAL_SUFFIX))

    @Synchronized
    fun read(downloadsRootUri: String?): BookDownloadIndexRestore? {
        lastReadRequiresVerification = false
        lastReadRequiresReconciliation = false
        if (downloadsRootUri == null) return null
        val snapshot = readSnapshot(indexFile)
            ?: migrateLegacySnapshot(downloadsRootUri)
            ?: return null
        if (snapshot.downloadsRootUri != downloadsRootUri) return null

        val packages = snapshot.packages.associateByTo(linkedMapOf()) { it.manifest.packageKey }
        val journalResult = journal.replay(downloadsRootUri, packages)
        return BookDownloadIndexRestore(
            packages = packages.values.toList(),
            requiresReconciliation = lastReadRequiresReconciliation ||
                journalResult.discardedTail ||
                reconciliationMarker.exists(),
        )
    }

    @Synchronized
    fun replace(downloadsRootUri: String?, packages: Collection<IndexedBookDownloadPackage>) {
        if (downloadsRootUri == null) return
        val snapshot = BookDownloadIndexSnapshot(
            downloadsRootUri = downloadsRootUri,
            packages = packages.sortedWith(PACKAGE_ORDER),
        )
        runCatching {
            writeSnapshot(snapshot)
            journal.clear()
            reconciliationMarker.delete()
        }.onFailure {
            markRequiresReconciliation()
        }.getOrThrow()
    }

    @Synchronized
    fun upsert(downloadsRootUri: String?, download: IndexedBookDownloadPackage) {
        if (downloadsRootUri == null) return
        runCatching {
            journal.append(
                BookDownloadIndexMutation(
                    downloadsRootUri = downloadsRootUri,
                    upserts = listOf(download),
                ),
            )
        }.onFailure { markRequiresReconciliation() }.getOrThrow()
    }

    @Synchronized
    fun remove(downloadsRootUri: String?, packageKeys: Collection<BookDownloadPackageKey>) {
        if (downloadsRootUri == null || packageKeys.isEmpty()) return
        runCatching {
            journal.append(
                BookDownloadIndexMutation(
                    downloadsRootUri = downloadsRootUri,
                    removals = packageKeys.map(BookDownloadIndexKey::from),
                ),
            )
        }.onFailure { markRequiresReconciliation() }.getOrThrow()
    }

    fun resolveDirectory(download: IndexedBookDownloadPackage): UniFile? =
        UniFile.fromUri(context, download.directoryUri.toUri())

    @Synchronized
    fun shouldCompact(): Boolean = journal.shouldCompact()

    private fun markRequiresReconciliation() {
        runCatching {
            reconciliationMarker.parentFile?.mkdirs()
            reconciliationMarker.writeBytes(byteArrayOf(1))
        }
    }

    private fun migrateLegacySnapshot(downloadsRootUri: String): BookDownloadIndexSnapshot? {
        val legacyFile = legacyCacheFile?.takeIf(File::exists) ?: return null
        val legacy = runCatching {
            BookDownloadAtomicFile(legacyFile).openRead().use {
                ProtoBuf.decodeFromByteArray<LegacyBookDownloadIndexSnapshot>(it.readBytes())
            }
        }.getOrNull() ?: run {
            lastReadRequiresVerification = true
            return null
        }
        if (legacy.version != LEGACY_VERSION || legacy.downloadsRootUri != downloadsRootUri) return null

        val migrated = BookDownloadIndexSnapshot(
            downloadsRootUri = downloadsRootUri,
            packages = legacy.packages.map { packageRecord ->
                IndexedBookDownloadPackage(
                    manifest = BookDownloadIndexManifest.from(packageRecord.manifest),
                    directoryUri = packageRecord.directoryUri,
                )
            },
        )
        writeSnapshot(migrated)
        journal.clear()
        lastReadRequiresReconciliation = true
        markRequiresReconciliation()
        BookDownloadAtomicFile(legacyFile).delete()
        return migrated
    }

    private fun readSnapshot(file: File): BookDownloadIndexSnapshot? {
        val atomicFile = BookDownloadAtomicFile(file)
        if (!atomicFile.exists()) return null
        return runCatching {
            atomicFile.openRead().use {
                ProtoBuf.decodeFromByteArray<BookDownloadIndexSnapshot>(it.readBytes())
            }.also { snapshot ->
                require(snapshot.version == CURRENT_VERSION) { "Unsupported BOOK download index version" }
            }
        }.getOrElse {
            lastReadRequiresVerification = true
            null
        }
    }

    private fun writeSnapshot(snapshot: BookDownloadIndexSnapshot) {
        indexFile.parentFile?.mkdirs()
        BookDownloadAtomicFile(indexFile).write(ProtoBuf.encodeToByteArray(snapshot))
    }

    private companion object {
        const val INDEX_FILE_NAME = "book_download_index_v2"
        const val LEGACY_CACHE_FILE_NAME = "book_dl_index_cache_v1"
        const val JOURNAL_SUFFIX = ".journal"
        const val RECONCILIATION_SUFFIX = ".reconcile"
        const val CURRENT_VERSION = 2
        const val LEGACY_VERSION = 1

        val PACKAGE_ORDER = compareBy<IndexedBookDownloadPackage> { it.manifest.sourceId }
            .thenBy { it.manifest.entryUrl }
            .thenBy { it.manifest.childUrl }
    }
}

private fun Context.durableBookDownloadDirectory(): File =
    runCatching { noBackupFilesDir }.getOrNull()
        ?.takeIf { directory -> runCatching { directory.path.isNotBlank() }.getOrDefault(false) }
        ?: filesDir

internal data class BookDownloadIndexRestore(
    val packages: List<IndexedBookDownloadPackage>,
    val requiresReconciliation: Boolean,
)

@Serializable
internal data class IndexedBookDownloadPackage(
    val manifest: BookDownloadIndexManifest,
    val directoryUri: String,
) {
    companion object {
        fun from(download: VerifiedBookDownloadPackage) = IndexedBookDownloadPackage(
            manifest = BookDownloadIndexManifest.from(download.manifest),
            directoryUri = download.directory.uri.toString(),
        )
    }
}

@Serializable
internal data class BookDownloadIndexManifest(
    val sourceId: Long,
    val entryId: Long,
    val entryTitle: String,
    val entryUrl: String,
    val childId: Long,
    val childTitle: String,
    val childUrl: String,
    val createdAt: Long,
    val publicationId: String? = null,
) {
    val packageKey: BookDownloadPackageKey
        get() = BookDownloadPackageKey(sourceId, entryUrl, childUrl)

    companion object {
        fun from(manifest: BookDownloadManifest) = BookDownloadIndexManifest(
            sourceId = manifest.sourceId,
            entryId = manifest.entryId,
            entryTitle = manifest.entryTitle,
            entryUrl = manifest.entryUrl,
            childId = manifest.childId,
            childTitle = manifest.childTitle,
            childUrl = manifest.childUrl,
            createdAt = manifest.createdAt,
            publicationId = manifest.publicationId,
        )
    }
}

@Serializable
internal data class BookDownloadIndexMutation(
    val version: Int = 1,
    val downloadsRootUri: String,
    val upserts: List<IndexedBookDownloadPackage> = emptyList(),
    val removals: List<BookDownloadIndexKey> = emptyList(),
)

@Serializable
internal data class BookDownloadIndexKey(
    val sourceId: Long,
    val entryUrl: String,
    val childUrl: String,
) {
    fun toPackageKey() = BookDownloadPackageKey(sourceId, entryUrl, childUrl)

    companion object {
        fun from(key: BookDownloadPackageKey) = BookDownloadIndexKey(key.sourceId, key.entryUrl, key.childUrl)
    }
}

@Serializable
private data class BookDownloadIndexSnapshot(
    val version: Int = 2,
    val downloadsRootUri: String,
    val packages: List<IndexedBookDownloadPackage>,
)

@Serializable
private data class LegacyBookDownloadIndexSnapshot(
    val version: Int = 1,
    val downloadsRootUri: String,
    val packages: List<LegacyBookDownloadIndexPackage>,
)

@Serializable
private data class LegacyBookDownloadIndexPackage(
    val manifest: BookDownloadManifest,
    val directoryUri: String,
    val resourceUris: Map<String, String>,
)
