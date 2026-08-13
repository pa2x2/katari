package mihon.entry.interactions.book.download

import com.hippo.unifile.UniFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/** Reconciles app-private BOOK metadata against the authoritative download directory hierarchy. */
internal class BookDownloadReconciler(
    private val downloadsDirectory: () -> UniFile?,
    private val directoryListing: BookDownloadDirectoryListing,
    private val reconciliationStore: BookDownloadReconciliationStore?,
    private val readIndexedPackage: (UniFile, List<BookDownloadDirectoryEntry>) -> IndexedBookDownloadPackage?,
) {
    suspend fun discoverPackages(
        onPackage: (IndexedBookDownloadPackage) -> Unit,
    ): BookDownloadIndexScan = coroutineScope {
        val root = downloadsDirectory() ?: return@coroutineScope BookDownloadIndexScan(emptyList(), 0)
        val downloadsRootUri = root.uri.toString()
        val sourceDirectories = directoryListing.list(root)
            .filter {
                it.isDirectory && it.name?.endsWith(BookDownloadProvider.SOURCE_DIRECTORY_SUFFIX) == true
            }
            .map(BookDownloadDirectoryEntry::file)
        val cachedSources = reconciliationStore?.read(downloadsRootUri).orEmpty()
        val discoveries = sourceDirectories.map { sourceDirectory ->
            discoverSourceDirectory(
                sourceDirectory = sourceDirectory,
                cachedSource = cachedSources[sourceDirectory.uri.toString()],
                onPackage = onPackage,
            )
        }
        val reconciliationSources = discoveries.map(BookDownloadSourceDiscovery::record)
        if (reconciliationSources.associateBy(BookDownloadReconciliationSource::directoryUri) != cachedSources) {
            reconciliationStore?.replace(downloadsRootUri, reconciliationSources)
        }
        val scans = discoveries.map(BookDownloadSourceDiscovery::scan)
        BookDownloadIndexScan(
            packages = scans.flatMap(BookDownloadDirectoryScan<IndexedBookDownloadPackage>::packages),
            invalidPackageCount = scans.sumOf(
                BookDownloadDirectoryScan<IndexedBookDownloadPackage>::invalidPackageCount,
            ),
            cleanedTemporaryPackageCount = scans.sumOf(
                BookDownloadDirectoryScan<IndexedBookDownloadPackage>::cleanedTemporaryPackageCount,
            ),
        )
    }

    private suspend fun discoverSourceDirectory(
        sourceDirectory: UniFile,
        cachedSource: BookDownloadReconciliationSource?,
        onPackage: (IndexedBookDownloadPackage) -> Unit,
    ): BookDownloadSourceDiscovery {
        val sourceContents = directoryListing.list(sourceDirectory)
        val cachedEntries = cachedSource?.entries.orEmpty().associateBy(BookDownloadReconciliationEntry::directoryName)
        var invalidPackages = 0
        var cleanedTemporaryPackages = 0
        val reconciliationEntries = mutableListOf<BookDownloadReconciliationEntry>()
        val packages = buildList {
            sourceContents.filter(BookDownloadDirectoryEntry::isDirectory).forEach { entryDirectory ->
                val directoryName = entryDirectory.name ?: return@forEach
                val cachedEntry = cachedEntries[directoryName]
                val entryDiscovery = discoverEntryDirectory(
                    entryDirectory = entryDirectory.file,
                    cachedPackages = cachedEntry?.packages.orEmpty(),
                    onPackage = onPackage,
                )
                invalidPackages += entryDiscovery.scan.invalidPackageCount
                cleanedTemporaryPackages += entryDiscovery.scan.cleanedTemporaryPackageCount
                addAll(entryDiscovery.scan.packages)
                reconciliationEntries += BookDownloadReconciliationEntry(
                    directoryName = directoryName,
                    directoryLastModified = entryDirectory.lastModified,
                    packages = entryDiscovery.packages,
                )
            }
        }
        return BookDownloadSourceDiscovery(
            scan = BookDownloadDirectoryScan(packages, invalidPackages, cleanedTemporaryPackages),
            record = BookDownloadReconciliationSource(
                directoryUri = sourceDirectory.uri.toString(),
                entries = reconciliationEntries,
            ),
        )
    }

    private suspend fun discoverEntryDirectory(
        entryDirectory: UniFile,
        cachedPackages: Collection<BookDownloadReconciliationPackage>,
        onPackage: (IndexedBookDownloadPackage) -> Unit,
    ): BookDownloadEntryDiscovery {
        var invalidPackages = 0
        var cleanedTemporaryPackages = 0
        val childDirectories = directoryListing.list(entryDirectory).filter(BookDownloadDirectoryEntry::isDirectory)
        val completeDirectories = childDirectories
            .filterTo(mutableListOf()) { childDirectory ->
                childDirectory.name?.endsWith(BookDownloadProvider.STAGING_SUFFIX) != true &&
                    childDirectory.name?.endsWith(BookDownloadProvider.BACKUP_SUFFIX) != true
            }

        childDirectories
            .filter { it.name?.endsWith(BookDownloadProvider.STAGING_SUFFIX) == true }
            .forEach { if (it.file.delete()) cleanedTemporaryPackages++ }
        val completeNames = completeDirectories.mapNotNullTo(mutableSetOf(), BookDownloadDirectoryEntry::name)
        childDirectories
            .filter { it.name?.endsWith(BookDownloadProvider.BACKUP_SUFFIX) == true }
            .forEach { backup ->
                val finalName = checkNotNull(backup.name).removeSuffix(BookDownloadProvider.BACKUP_SUFFIX)
                if (finalName in completeNames) {
                    if (backup.file.delete()) cleanedTemporaryPackages++
                } else if (backup.file.renameTo(finalName)) {
                    cleanedTemporaryPackages++
                    completeNames += finalName
                    completeDirectories += backup.copy(name = finalName)
                }
            }

        val cachedByDirectory = cachedPackages.associateBy(BookDownloadReconciliationPackage::directoryName)
        val packageRecords = mutableListOf<BookDownloadReconciliationPackage>()
        val packagesRequiringManifest = mutableListOf<BookDownloadDirectoryEntry>()
        val packages = buildList {
            completeDirectories.forEach { childDirectory ->
                val directoryName = childDirectory.name
                if (directoryName == null) {
                    invalidPackages++
                    return@forEach
                }
                val cached = cachedByDirectory[directoryName]?.takeIf { it.matches(childDirectory) }
                if (cached == null) {
                    packagesRequiringManifest += childDirectory
                } else {
                    val download = cached.download.copy(directoryUri = childDirectory.file.uri.toString())
                    add(download)
                    packageRecords += cached.copy(download = download)
                    onPackage(download)
                }
            }

            packagesRequiringManifest.chunked(DISCOVERY_CONCURRENCY).forEach { batch ->
                val discoveredBatch = coroutineScope {
                    batch.map { childDirectory ->
                        async(Dispatchers.IO) {
                            val contents = directoryListing.list(childDirectory.file)
                            readIndexedPackage(childDirectory.file, contents)?.let { download ->
                                BookDownloadPackageDiscovery(
                                    download = download,
                                    record = BookDownloadReconciliationPackage(
                                        directoryName = checkNotNull(childDirectory.name),
                                        directoryLastModified = childDirectory.lastModified,
                                        download = download,
                                    ),
                                )
                            }
                        }
                    }.awaitAll()
                }
                discoveredBatch.forEach { discovered ->
                    if (discovered == null) {
                        invalidPackages++
                    } else {
                        add(discovered.download)
                        packageRecords += discovered.record
                        onPackage(discovered.download)
                    }
                }
            }
        }
        return BookDownloadEntryDiscovery(
            scan = BookDownloadDirectoryScan(packages, invalidPackages, cleanedTemporaryPackages),
            packages = packageRecords,
        )
    }

    private companion object {
        const val DISCOVERY_CONCURRENCY = 16
    }
}

private data class BookDownloadSourceDiscovery(
    val scan: BookDownloadDirectoryScan<IndexedBookDownloadPackage>,
    val record: BookDownloadReconciliationSource,
)

private data class BookDownloadEntryDiscovery(
    val scan: BookDownloadDirectoryScan<IndexedBookDownloadPackage>,
    val packages: List<BookDownloadReconciliationPackage>,
)

private data class BookDownloadPackageDiscovery(
    val download: IndexedBookDownloadPackage,
    val record: BookDownloadReconciliationPackage,
)
