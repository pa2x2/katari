package mihon.entry.interactions.book.download

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.util.lang.Hash
import eu.kanade.tachiyomi.util.storage.DiskUtil
import kotlinx.serialization.json.Json
import mihon.book.api.BookContentResource
import mihon.entry.interactions.book.content.fileSuffix
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.storage.service.StorageManager
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest

/** Owns the on-disk package layout and verifies packages before exposing them to readers. */
internal class BookDownloadProvider(
    private val downloadsDirectory: () -> UniFile?,
    private val json: Json = manifestJson(),
    private val directoryListing: BookDownloadDirectoryListing = BookDownloadDirectoryListing(),
    private val reconciliationStore: BookDownloadReconciliationStore? = null,
) {
    private val reconciler = BookDownloadReconciler(
        downloadsDirectory = downloadsDirectory,
        directoryListing = directoryListing,
        reconciliationStore = reconciliationStore,
        readIndexedPackage = ::readIndexedPackage,
    )

    constructor(storageManager: StorageManager, context: Context, json: Json = manifestJson()) : this(
        downloadsDirectory = storageManager::getDownloadsDirectory,
        json = json,
        directoryListing = BookDownloadDirectoryListing(context),
        reconciliationStore = BookDownloadReconciliationStore(context),
    )

    fun downloadsRootUri(): String? = downloadsDirectory()?.uri?.toString()

    fun beginPackage(
        sourceName: String,
        entry: Entry,
        child: EntryChapter,
    ): Result<BookDownloadStagingPackage> = runCatching {
        val root = downloadsDirectory() ?: throw IOException("BOOK download directory is unavailable")
        val sourceDirectory = root.createDirectory(sourceDirectoryName(sourceName))
            ?: throw IOException("Unable to create BOOK source download directory")
        val entryDirectory = sourceDirectory.createDirectory(entryDirectoryName(entry))
            ?: throw IOException("Unable to create BOOK entry download directory")
        val finalName = childDirectoryName(child)
        val stagingName = finalName + STAGING_SUFFIX
        entryDirectory.findFile(stagingName)?.delete()
        val stagingDirectory = entryDirectory.createDirectory(stagingName)
            ?: throw IOException("Unable to create temporary BOOK download directory")
        BookDownloadStagingPackage(
            packageKey = BookDownloadPackageKey(entry.source, entry.url, child.url),
            parentDirectory = entryDirectory,
            directory = stagingDirectory,
            finalDirectoryName = finalName,
        )
    }

    fun resourceFileName(resourceId: String, mediaType: String?): String =
        Hash.sha256(resourceId).take(24) + resourceFileSuffix(mediaType)

    fun completePackage(
        staging: BookDownloadStagingPackage,
        manifest: BookDownloadManifest,
    ): Result<VerifiedBookDownloadPackage> = runCatching {
        require(staging.packageKey == manifest.packageKey) { "BOOK staging identity does not match its manifest" }
        verifyResources(directoryListing.list(staging.directory), manifest)
        check(staging.directory.findFile(MANIFEST_FILE_NAME) == null) { "BOOK staging manifest already exists" }
        val manifestFile = staging.directory.createFile(MANIFEST_FILE_NAME)
            ?: throw IOException("Unable to create BOOK download manifest")
        manifestFile.openOutputStream().buffered().use { output ->
            output.write(json.encodeToString(manifest).encodeToByteArray())
        }
        val verifiedStaging = readVerifiedPackage(staging.directory)
            ?: throw IOException("Completed BOOK staging package failed verification")

        val backupName = staging.finalDirectoryName + BACKUP_SUFFIX
        staging.parentDirectory.findFile(backupName)?.delete()
        val existing = staging.parentDirectory.findFile(staging.finalDirectoryName)
        if (existing != null && !existing.renameTo(backupName)) {
            throw IOException("Unable to preserve the existing BOOK download")
        }
        if (!staging.directory.renameTo(staging.finalDirectoryName)) {
            staging.parentDirectory.findFile(backupName)?.renameTo(staging.finalDirectoryName)
            throw IOException("Unable to publish the BOOK download")
        }
        val publishedDirectory = staging.parentDirectory.findFile(staging.finalDirectoryName)
        val published = publishedDirectory?.let(::readVerifiedPackage)
        if (published == null) {
            publishedDirectory?.delete()
            staging.parentDirectory.findFile(backupName)?.renameTo(staging.finalDirectoryName)
            throw IOException("Published BOOK download failed verification")
        }
        staging.parentDirectory.findFile(backupName)?.delete()
        check(verifiedStaging.manifest == published.manifest)
        published
    }

    fun scanPackages(): BookDownloadPackageScan {
        val root = downloadsDirectory() ?: return BookDownloadPackageScan(emptyList(), 0)
        val sourceDirectories = directoryListing.list(root)
            .filter { it.isDirectory && it.name?.endsWith(SOURCE_DIRECTORY_SUFFIX) == true }
            .map(BookDownloadDirectoryEntry::file)
        return scanSourceDirectories(sourceDirectories)
    }

    fun rebuildPackages(): BookDownloadPackageScan {
        val root = downloadsDirectory() ?: return BookDownloadPackageScan(emptyList(), 0)
        val sourceDirectories = directoryListing.list(root)
            .filter { it.isDirectory && it.name?.endsWith(SOURCE_DIRECTORY_SUFFIX) == true }
            .map(BookDownloadDirectoryEntry::file)
        val entryDirectories = sourceDirectories.flatMap { sourceDirectory ->
            directoryListing.list(sourceDirectory).filter(BookDownloadDirectoryEntry::isDirectory)
                .map(BookDownloadDirectoryEntry::file)
        }
        return scanEntryDirectories(entryDirectories, cleanupTemporaryPackages = true)
    }

    suspend fun discoverPackages(
        onPackage: (IndexedBookDownloadPackage) -> Unit = {},
    ): BookDownloadIndexScan = reconciler.discoverPackages(onPackage)

    fun scanSourcePackages(sourceName: String): BookDownloadPackageScan {
        val root = downloadsDirectory() ?: return BookDownloadPackageScan(emptyList(), 0)
        val sourceDirectory = root.findFile(sourceDirectoryName(sourceName))
            ?.takeIf(UniFile::isDirectory)
            ?: return BookDownloadPackageScan(emptyList(), 0)
        return scanSourceDirectories(listOf(sourceDirectory))
    }

    fun scanEntryPackages(sourceName: String, entry: Entry): BookDownloadPackageScan {
        val root = downloadsDirectory() ?: return BookDownloadPackageScan(emptyList(), 0)
        val sourceDirectory = root.findFile(sourceDirectoryName(sourceName))
            ?.takeIf(UniFile::isDirectory)
            ?: return BookDownloadPackageScan(emptyList(), 0)
        val entryDirectory = sourceDirectory.findFile(entryDirectoryName(entry))
            ?.takeIf(UniFile::isDirectory)
            ?: return BookDownloadPackageScan(emptyList(), 0)
        return scanEntryDirectories(listOf(entryDirectory))
    }

    /** Resolves one known package without waiting for a whole-library discovery pass. */
    fun findVerifiedPackage(entry: Entry, child: EntryChapter): VerifiedBookDownloadPackage? {
        val root = downloadsDirectory() ?: return null
        val packageKey = BookDownloadPackageKey(entry.source, entry.url, child.url)
        val entryDirectoryName = entryDirectoryName(entry)
        val childDirectoryName = childDirectoryName(child)
        return root.listFiles().orEmpty()
            .asSequence()
            .filter { it.isDirectory && it.name?.endsWith(SOURCE_DIRECTORY_SUFFIX) == true }
            .mapNotNull { sourceDirectory ->
                sourceDirectory.findFile(entryDirectoryName)?.takeIf(UniFile::isDirectory)
            }
            .mapNotNull { entryDirectory ->
                entryDirectory.findFile(childDirectoryName)?.takeIf(UniFile::isDirectory)
            }
            .mapNotNull(::readVerifiedPackage)
            .firstOrNull { it.manifest.packageKey == packageKey }
    }

    private fun scanSourceDirectories(sourceDirectories: Collection<UniFile>): BookDownloadPackageScan {
        val entryDirectories = sourceDirectories.flatMap { sourceDirectory ->
            directoryListing.list(sourceDirectory).filter(BookDownloadDirectoryEntry::isDirectory)
                .map(BookDownloadDirectoryEntry::file)
        }
        return scanEntryDirectories(entryDirectories)
    }

    private fun scanEntryDirectories(
        entryDirectories: Collection<UniFile>,
        cleanupTemporaryPackages: Boolean = false,
    ): BookDownloadPackageScan {
        val scan = scanEntryDirectories(
            entryDirectories = entryDirectories,
            cleanupTemporaryPackages = cleanupTemporaryPackages,
            readPackage = ::readVerifiedPackage,
        )
        return BookDownloadPackageScan(scan.packages, scan.invalidPackageCount, scan.cleanedTemporaryPackageCount)
    }

    private fun <Package> scanEntryDirectories(
        entryDirectories: Collection<UniFile>,
        cleanupTemporaryPackages: Boolean,
        readPackage: (UniFile, List<BookDownloadDirectoryEntry>) -> Package?,
        onPackage: (Package) -> Unit = {},
    ): BookDownloadDirectoryScan<Package> {
        var invalidPackages = 0
        var cleanedTemporaryPackages = 0
        val packages = buildList {
            entryDirectories.forEach { entryDirectory ->
                val entryContents = directoryListing.list(entryDirectory)
                val childDirectories = entryContents.filter(BookDownloadDirectoryEntry::isDirectory)
                val completeDirectories = childDirectories
                    .filterTo(mutableListOf()) { childDirectory ->
                        childDirectory.name?.endsWith(STAGING_SUFFIX) != true &&
                            childDirectory.name?.endsWith(BACKUP_SUFFIX) != true
                    }

                if (cleanupTemporaryPackages) {
                    childDirectories
                        .filter { it.name?.endsWith(STAGING_SUFFIX) == true }
                        .forEach { if (it.file.delete()) cleanedTemporaryPackages++ }
                    val completeNames = completeDirectories.mapNotNullTo(
                        mutableSetOf(),
                        BookDownloadDirectoryEntry::name,
                    )
                    childDirectories
                        .filter { it.name?.endsWith(BACKUP_SUFFIX) == true }
                        .forEach { backup ->
                            val finalName = checkNotNull(backup.name).removeSuffix(BACKUP_SUFFIX)
                            if (finalName in completeNames) {
                                if (backup.file.delete()) cleanedTemporaryPackages++
                            } else if (backup.file.renameTo(finalName)) {
                                cleanedTemporaryPackages++
                                completeNames += finalName
                                completeDirectories += backup.copy(name = finalName)
                            }
                        }
                }

                completeDirectories.forEach { childDirectory ->
                    val contents = directoryListing.list(childDirectory.file)
                    if (contents.none { it.name == MANIFEST_FILE_NAME && !it.isDirectory }) return@forEach
                    val discovered = readPackage(childDirectory.file, contents)
                    if (discovered == null) {
                        invalidPackages++
                    } else {
                        add(discovered)
                        onPackage(discovered)
                    }
                }
            }
        }
        return BookDownloadDirectoryScan(packages, invalidPackages, cleanedTemporaryPackages)
    }

    fun renameSource(oldName: String, newName: String): Boolean {
        val root = downloadsDirectory() ?: return false
        val oldDirectory = root.findFile(sourceDirectoryName(oldName)) ?: return false
        val newDirectoryName = sourceDirectoryName(newName)
        if (oldDirectory.name == newDirectoryName) return true
        if (oldDirectory.name?.equals(newDirectoryName, ignoreCase = true) == true) {
            val originalName = checkNotNull(oldDirectory.name)
            if (!oldDirectory.renameTo(newDirectoryName + STAGING_SUFFIX)) return false
            if (oldDirectory.renameTo(newDirectoryName)) return true
            oldDirectory.renameTo(originalName)
            return false
        }
        if (root.findFile(newDirectoryName) != null) return false
        return oldDirectory.renameTo(newDirectoryName)
    }

    fun renameEntry(sourceName: String, entry: Entry, newTitle: String): Boolean {
        val root = downloadsDirectory() ?: return false
        val sourceDirectory = root.findFile(sourceDirectoryName(sourceName)) ?: return false
        val oldDirectory = sourceDirectory.findFile(entryDirectoryName(entry)) ?: return false
        val newDirectoryName = stableDirectoryName(newTitle, entry.url, "Book")
        if (oldDirectory.name == newDirectoryName) return true
        if (oldDirectory.name?.equals(newDirectoryName, ignoreCase = true) == true) {
            val originalName = checkNotNull(oldDirectory.name)
            if (!oldDirectory.renameTo(newDirectoryName + STAGING_SUFFIX)) return false
            if (oldDirectory.renameTo(newDirectoryName)) return true
            oldDirectory.renameTo(originalName)
            return false
        }
        if (sourceDirectory.findFile(newDirectoryName) != null) return false
        return oldDirectory.renameTo(newDirectoryName)
    }

    internal fun readVerifiedPackage(directory: UniFile): VerifiedBookDownloadPackage? =
        readVerifiedPackage(directory, directoryListing.list(directory))

    private fun readVerifiedPackage(
        directory: UniFile,
        contents: List<BookDownloadDirectoryEntry>,
    ): VerifiedBookDownloadPackage? = runCatching {
        val manifest = readManifest(contents)
        val resources = verifyResources(contents, manifest)
        VerifiedBookDownloadPackage(directory, manifest, resources)
    }.getOrNull()

    private fun readIndexedPackage(
        directory: UniFile,
        contents: List<BookDownloadDirectoryEntry>,
    ): IndexedBookDownloadPackage? = runCatching {
        IndexedBookDownloadPackage(
            manifest = BookDownloadIndexManifest.from(readManifest(contents)),
            directoryUri = directory.uri.toString(),
        )
    }.getOrNull()

    private fun readManifest(contents: List<BookDownloadDirectoryEntry>): BookDownloadManifest {
        val manifestEntry = contents.firstOrNull { it.name == MANIFEST_FILE_NAME && !it.isDirectory }
            ?: throw IOException("BOOK download manifest is missing")
        val manifestLength = manifestEntry.length
        require(manifestLength <= MAX_MANIFEST_BYTES) { "BOOK download manifest is too large" }
        val manifestText = manifestEntry.file.openInputStream().use { it.readBoundedText(MAX_MANIFEST_BYTES) }
        require(manifestText.isNotEmpty()) { "BOOK download manifest is empty" }
        return json.decodeFromString<BookDownloadManifest>(manifestText)
    }

    private fun verifyResources(
        contents: List<BookDownloadDirectoryEntry>,
        manifest: BookDownloadManifest,
    ): Map<String, UniFile> {
        val filesByName = contents.filterNot(
            BookDownloadDirectoryEntry::isDirectory,
        ).associateBy(BookDownloadDirectoryEntry::name)
        return manifest.resources.associate { resource ->
            val entry = filesByName[resource.fileName]
                ?: throw IOException("Downloaded BOOK resource ${resource.id} is missing")
            val storedSize = entry.length.takeIf { it >= 0L } ?: entry.file.length()
            require(storedSize == resource.storedSize) {
                "Downloaded BOOK resource ${resource.id} has the wrong size"
            }
            val digest = entry.file.openInputStream().use(InputStream::sha256)
            require(digest == resource.sha256) {
                "Downloaded BOOK resource ${resource.id} failed integrity verification"
            }
            resource.id to entry.file
        }
    }

    private fun sourceDirectoryName(sourceName: String): String =
        DiskUtil.buildValidFilename(sourceName + SOURCE_DIRECTORY_SUFFIX)

    private fun entryDirectoryName(entry: Entry): String = stableDirectoryName(entry.title, entry.url, "Book")

    private fun childDirectoryName(child: EntryChapter): String = stableDirectoryName(child.name, child.url, "Chapter")

    private fun stableDirectoryName(title: String, stableKey: String, fallback: String): String {
        val safeTitle = DiskUtil.buildValidFilename(title.ifBlank { fallback }, DiskUtil.MAX_FILE_NAME_BYTES - 18)
        return safeTitle + "_" + Hash.sha256(stableKey).take(16)
    }

    companion object {
        const val MANIFEST_FILE_NAME = "book_download.json"
        const val SOURCE_DIRECTORY_SUFFIX = " [book]"
        const val STAGING_SUFFIX = ".booktmp"
        const val BACKUP_SUFFIX = ".bookbak"
        const val MAX_MANIFEST_BYTES = 1024L * 1024L

        fun manifestJson(): Json = Json {
            encodeDefaults = true
            ignoreUnknownKeys = false
        }
    }
}

internal data class BookDownloadStagingPackage(
    val packageKey: BookDownloadPackageKey,
    val parentDirectory: UniFile,
    val directory: UniFile,
    val finalDirectoryName: String,
)

/** Package handles verified by the provider, or pending one-time verification after index restoration. */
internal data class VerifiedBookDownloadPackage(
    val directory: UniFile,
    val manifest: BookDownloadManifest,
    val resources: Map<String, UniFile>,
)

internal data class BookDownloadPackageScan(
    val packages: List<VerifiedBookDownloadPackage>,
    val invalidPackageCount: Int,
    val cleanedTemporaryPackageCount: Int = 0,
)

internal data class BookDownloadIndexScan(
    val packages: List<IndexedBookDownloadPackage>,
    val invalidPackageCount: Int,
    val cleanedTemporaryPackageCount: Int = 0,
)

internal data class BookDownloadDirectoryScan<Package>(
    val packages: List<Package>,
    val invalidPackageCount: Int,
    val cleanedTemporaryPackageCount: Int,
)

private fun resourceFileSuffix(mediaType: String?): String = when (mediaType) {
    null -> ".bin"
    else -> BookContentResource(id = "resource", mediaType = mediaType).fileSuffix()
}

private fun InputStream.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        digest.update(buffer, 0, read)
    }
    return digest.digest().joinToString("") { byte ->
        (byte.toInt() and 0xff).toString(16).padStart(2, '0')
    }
}

private fun InputStream.readBoundedText(maxBytes: Long): String {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        total += read
        require(total <= maxBytes) { "BOOK download manifest is too large" }
        output.write(buffer, 0, read)
    }
    return output.toString(Charsets.UTF_8.name())
}
