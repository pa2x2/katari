package mihon.entry.interactions.book.download

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryMerge

/** Verified in-memory index of durable BOOK packages. The filesystem remains the source of truth. */
internal class BookDownloadCache(
    private val provider: BookDownloadProvider,
) {
    private val refreshMutex = Mutex()

    @Volatile
    private var initialized = false
    private val _packages = MutableStateFlow<Map<BookDownloadPackageKey, VerifiedBookDownloadPackage>>(emptyMap())
    val packages: StateFlow<Map<BookDownloadPackageKey, VerifiedBookDownloadPackage>> = _packages.asStateFlow()
    private val mergedMemberIdsState = MutableStateFlow<Map<Long, Set<Long>>>(emptyMap())
    val changes: Flow<Unit> = merge(
        _packages.drop(1).map { Unit },
        mergedMemberIdsState.drop(1).map { Unit },
    )

    private val _isInitializing = MutableStateFlow(false)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    suspend fun ensureInitialized() {
        if (initialized) return
        refreshMutex.withLock {
            if (!initialized) refreshLocked(reportInitialization = true)
        }
    }

    suspend fun refresh(reportInitialization: Boolean = false): BookDownloadCacheRefresh = refreshMutex.withLock {
        refreshLocked(reportInitialization)
    }

    private suspend fun refreshLocked(reportInitialization: Boolean): BookDownloadCacheRefresh =
        withContext(Dispatchers.IO) {
            if (reportInitialization) _isInitializing.value = true
            try {
                val cleaned = provider.cleanupTemporaryPackages()
                val scan = provider.scanPackages()
                val selected = scan.packages
                    .groupBy { it.manifest.packageKey }
                    .mapValues { (_, packages) ->
                        packages.maxWith(
                            compareBy<VerifiedBookDownloadPackage> { it.manifest.createdAt }
                                .thenBy { it.directory.uri.toString() },
                        )
                    }
                _packages.value = selected
                initialized = true
                BookDownloadCacheRefresh(
                    packageCount = selected.size,
                    invalidPackageCount = scan.invalidPackageCount,
                    duplicatePackageCount = scan.packages.size - selected.size,
                    cleanedTemporaryPackageCount = cleaned,
                )
            } finally {
                if (reportInitialization) _isInitializing.value = false
            }
        }

    fun get(packageKey: BookDownloadPackageKey): VerifiedBookDownloadPackage? = _packages.value[packageKey]

    fun find(sourceId: Long, childUrl: String, entryTitle: String): VerifiedBookDownloadPackage? {
        val candidates = _packages.value.values.filter {
            it.manifest.sourceId == sourceId && it.manifest.childUrl == childUrl
        }
        return candidates.singleOrNull()
            ?: candidates.firstOrNull { it.manifest.entryTitle == entryTitle }
    }

    fun isDownloaded(packageKey: BookDownloadPackageKey): Boolean = packageKey in _packages.value

    fun getDownloadCount(sourceId: Long, entryUrl: String): Int =
        _packages.value.keys.count { it.sourceId == sourceId && it.entryUrl == entryUrl }

    fun getDownloadCount(entry: Entry): Int {
        val memberIds = memberEntryIds(entry.id)
        return _packages.value.values.count { download ->
            val manifest = download.manifest
            (manifest.sourceId == entry.source && manifest.entryUrl == entry.url) || manifest.entryId in memberIds
        }
    }

    fun memberEntryIds(entryId: Long): Set<Long> = mergedMemberIdsState.value[entryId] ?: setOf(entryId)

    fun updateMergedEntries(merges: List<EntryMerge>) {
        mergedMemberIdsState.value = merges
            .groupBy(EntryMerge::targetId)
            .values
            .flatMap { group ->
                val memberIds = group.mapTo(linkedSetOf(), EntryMerge::entryId)
                memberIds.map { it to memberIds }
            }
            .toMap()
    }

    fun getTotalDownloadCount(): Int = _packages.value.size
}

internal data class BookDownloadCacheRefresh(
    val packageCount: Int,
    val invalidPackageCount: Int,
    val duplicatePackageCount: Int,
    val cleanedTemporaryPackageCount: Int,
)
