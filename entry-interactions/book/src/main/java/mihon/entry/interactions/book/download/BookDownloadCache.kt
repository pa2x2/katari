package mihon.entry.interactions.book.download

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Verified in-memory index of durable BOOK packages. The filesystem remains the source of truth. */
internal class BookDownloadCache(
    private val provider: BookDownloadProvider,
) {
    private val refreshMutex = Mutex()
    private val _packages = MutableStateFlow<Map<BookDownloadPackageKey, VerifiedBookDownloadPackage>>(emptyMap())
    val packages: StateFlow<Map<BookDownloadPackageKey, VerifiedBookDownloadPackage>> = _packages.asStateFlow()
    val changes: Flow<Unit> = _packages.drop(1).map { Unit }

    private val _isInitializing = MutableStateFlow(false)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    suspend fun refresh(): BookDownloadCacheRefresh = refreshMutex.withLock {
        withContext(Dispatchers.IO) {
            _isInitializing.value = true
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
                BookDownloadCacheRefresh(
                    packageCount = selected.size,
                    invalidPackageCount = scan.invalidPackageCount,
                    duplicatePackageCount = scan.packages.size - selected.size,
                    cleanedTemporaryPackageCount = cleaned,
                )
            } finally {
                _isInitializing.value = false
            }
        }
    }

    fun get(packageKey: BookDownloadPackageKey): VerifiedBookDownloadPackage? = _packages.value[packageKey]

    fun isDownloaded(packageKey: BookDownloadPackageKey): Boolean = packageKey in _packages.value

    fun getDownloadCount(sourceId: Long, entryUrl: String): Int =
        _packages.value.keys.count { it.sourceId == sourceId && it.entryUrl == entryUrl }

    fun getTotalDownloadCount(): Int = _packages.value.size
}

internal data class BookDownloadCacheRefresh(
    val packageCount: Int,
    val invalidPackageCount: Int,
    val duplicatePackageCount: Int,
    val cleanedTemporaryPackageCount: Int,
)
