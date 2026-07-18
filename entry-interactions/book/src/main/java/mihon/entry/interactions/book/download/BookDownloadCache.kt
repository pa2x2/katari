package mihon.entry.interactions.book.download

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryMerge

/** Persisted index of durable BOOK packages. The filesystem remains the source of truth. */
internal class BookDownloadCache(
    private val provider: BookDownloadProvider,
    private val indexStore: BookDownloadIndexStore? = null,
    storageChanges: Flow<Unit> = emptyFlow(),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshMutex = Mutex()

    @Volatile
    private var initialized = false
    private val _packages = MutableStateFlow<Map<BookDownloadPackageKey, VerifiedBookDownloadPackage>>(emptyMap())
    val packages: StateFlow<Map<BookDownloadPackageKey, VerifiedBookDownloadPackage>> = _packages.asStateFlow()
    private val verifiedPackageKeys = mutableSetOf<BookDownloadPackageKey>()
    private val mergedMemberIdsState = MutableStateFlow<Map<Long, Set<Long>>>(emptyMap())
    val changes: Flow<Unit> = merge(
        _packages.drop(1).map { Unit },
        mergedMemberIdsState.drop(1).map { Unit },
    )

    private val _isInitializing = MutableStateFlow(false)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    init {
        storageChanges
            .onEach { refresh(reportInitialization = true) }
            .launchIn(scope)
    }

    suspend fun ensureInitialized() {
        if (initialized) return
        refreshMutex.withLock {
            if (!initialized) {
                val restored = withContext(Dispatchers.IO) {
                    indexStore?.read(provider.downloadsRootUri())
                }
                if (restored != null) {
                    _packages.value = selectPackages(restored)
                    verifiedPackageKeys.clear()
                    initialized = true
                } else {
                    refreshLocked(reportInitialization = true)
                }
            }
        }
    }

    suspend fun refresh(reportInitialization: Boolean = false): BookDownloadCacheRefresh = refreshMutex.withLock {
        refreshLocked(reportInitialization)
    }

    private suspend fun refreshLocked(reportInitialization: Boolean): BookDownloadCacheRefresh =
        withContext(Dispatchers.IO) {
            if (reportInitialization) _isInitializing.value = true
            try {
                val scan = provider.rebuildPackages()
                val selected = selectPackages(scan.packages)
                _packages.value = selected
                verifiedPackageKeys.clear()
                verifiedPackageKeys += selected.keys
                initialized = true
                persistLocked()
                BookDownloadCacheRefresh(
                    packageCount = selected.size,
                    invalidPackageCount = scan.invalidPackageCount,
                    duplicatePackageCount = scan.packages.size - selected.size,
                    cleanedTemporaryPackageCount = scan.cleanedTemporaryPackageCount,
                )
            } finally {
                if (reportInitialization) _isInitializing.value = false
            }
        }

    fun get(packageKey: BookDownloadPackageKey): VerifiedBookDownloadPackage? = _packages.value[packageKey]

    suspend fun getVerified(packageKey: BookDownloadPackageKey): VerifiedBookDownloadPackage? {
        ensureInitialized()
        return refreshMutex.withLock {
            val indexed = _packages.value[packageKey] ?: return@withLock null
            if (packageKey in verifiedPackageKeys) return@withLock indexed

            val verified = withContext(Dispatchers.IO) {
                provider.readVerifiedPackage(indexed.directory)
            }?.takeIf { it.manifest == indexed.manifest }
            if (verified == null) {
                _packages.value -= packageKey
                persistLocked()
                null
            } else {
                _packages.value += packageKey to verified
                verifiedPackageKeys += packageKey
                verified
            }
        }
    }

    suspend fun upsert(download: VerifiedBookDownloadPackage) {
        ensureInitialized()
        refreshMutex.withLock {
            val packageKey = download.manifest.packageKey
            _packages.value += packageKey to download
            verifiedPackageKeys += packageKey
            persistLocked()
        }
    }

    suspend fun remove(packageKeys: Collection<BookDownloadPackageKey>) {
        if (packageKeys.isEmpty()) return
        ensureInitialized()
        refreshMutex.withLock {
            _packages.value -= packageKeys.toSet()
            verifiedPackageKeys -= packageKeys.toSet()
            persistLocked()
        }
    }

    suspend fun replace(
        packageKeys: Collection<BookDownloadPackageKey>,
        downloads: Collection<VerifiedBookDownloadPackage>,
    ) {
        ensureInitialized()
        refreshMutex.withLock {
            val removedKeys = packageKeys.toSet()
            val replacements = selectPackages(downloads)
            _packages.value = (_packages.value - removedKeys) + replacements
            verifiedPackageKeys -= removedKeys
            verifiedPackageKeys += replacements.keys
            persistLocked()
        }
    }

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

    private fun selectPackages(
        downloads: Collection<VerifiedBookDownloadPackage>,
    ): Map<BookDownloadPackageKey, VerifiedBookDownloadPackage> = downloads
        .groupBy { it.manifest.packageKey }
        .mapValues { (_, packages) ->
            packages.maxWith(
                compareBy<VerifiedBookDownloadPackage> { it.manifest.createdAt }
                    .thenBy { it.directory.uri.toString() },
            )
        }

    private suspend fun persistLocked() = withContext(Dispatchers.IO) {
        runCatching {
            indexStore?.write(provider.downloadsRootUri(), _packages.value.values)
        }.onFailure { error ->
            logcat(LogPriority.ERROR, error) { "Failed to persist BOOK download index" }
        }
    }
}

internal data class BookDownloadCacheRefresh(
    val packageCount: Int,
    val invalidPackageCount: Int,
    val duplicatePackageCount: Int,
    val cleanedTemporaryPackageCount: Int,
)
