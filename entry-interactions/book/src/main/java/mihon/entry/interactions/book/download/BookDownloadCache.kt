package mihon.entry.interactions.book.download

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import java.util.concurrent.ConcurrentHashMap

/** Durable BOOK package metadata with lazy content verification and incremental lookup indexes. */
internal class BookDownloadCache(
    private val provider: BookDownloadProvider,
    private val indexStore: BookDownloadIndexStore? = null,
    storageChanges: Flow<Unit> = emptyFlow(),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshMutex = Mutex()
    private val stateLock = Any()
    private val packageVerificationLocks = ConcurrentHashMap<BookDownloadPackageKey, Mutex>()

    @Volatile
    private var initialized = false
    private val initialSnapshotReady = CompletableDeferred<Unit>()
    private val packages = linkedMapOf<BookDownloadPackageKey, IndexedBookDownloadPackage>()
    private val verifiedPackages = mutableMapOf<BookDownloadPackageKey, VerifiedBookDownloadPackage>()
    private val packageKeysByChild = mutableMapOf<BookDownloadChildKey, MutableSet<BookDownloadPackageKey>>()
    private val packageKeysByEntryIdentity = mutableMapOf<BookDownloadEntryKey, MutableSet<BookDownloadPackageKey>>()
    private val packageKeysByEntryId = mutableMapOf<Long, MutableSet<BookDownloadPackageKey>>()

    private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val changes: Flow<Unit> = _changes.asSharedFlow()
    private val _packageUpdates = MutableSharedFlow<BookDownloadPackageUpdate>(extraBufferCapacity = 128)
    val packageUpdates: Flow<BookDownloadPackageUpdate> = _packageUpdates.asSharedFlow()

    private val _isInitializing = MutableStateFlow(false)
    val isInitializing = _isInitializing.asStateFlow()

    init {
        if (indexStore != null) scope.launch { ensureInitialized() }
        storageChanges
            .onEach { reconcile(reportInitialization = true) }
            .launchIn(scope)
    }

    suspend fun awaitInitialSnapshot() {
        initialSnapshotReady.await()
    }

    suspend fun ensureInitialized() {
        if (initialized) return
        refreshMutex.withLock {
            if (initialized) return@withLock
            _isInitializing.value = true
            try {
                val restored = withContext(Dispatchers.IO) {
                    indexStore?.read(provider.downloadsRootUri())
                }
                if (restored != null) {
                    replaceState(restored.packages, emptyMap())
                    initialized = true
                    initialSnapshotReady.complete(Unit)
                    if (restored.requiresReconciliation) {
                        discoverFilesystemLocked()
                    }
                } else if (indexStore?.lastReadRequiresVerification == true) {
                    verifyFilesystemLocked(reportInitialization = false)
                } else {
                    discoverFilesystemLocked()
                }
            } finally {
                initialized = true
                initialSnapshotReady.complete(Unit)
                _isInitializing.value = false
            }
        }
    }

    suspend fun refresh(reportInitialization: Boolean = false): BookDownloadCacheRefresh = refreshMutex.withLock {
        verifyFilesystemLocked(reportInitialization)
    }

    private suspend fun reconcile(reportInitialization: Boolean): BookDownloadCacheRefresh = refreshMutex.withLock {
        if (reportInitialization) _isInitializing.value = true
        try {
            discoverFilesystemLocked()
        } finally {
            if (reportInitialization) _isInitializing.value = false
        }
    }

    private suspend fun discoverFilesystemLocked(): BookDownloadCacheRefresh = withContext(Dispatchers.IO) {
        var published = 0
        val scan = provider.discoverPackages { discovered ->
            val selected = synchronized(stateLock) {
                val current = packages[discovered.manifest.packageKey]
                if (current == null || discovered.isNewerThan(current)) {
                    putStateLocked(discovered, verified = null)
                    true
                } else {
                    false
                }
            }
            if (selected) {
                published++
                _packageUpdates.tryEmit(BookDownloadPackageUpdate(discovered.manifest, downloaded = true))
                if (published >= INITIAL_DISCOVERY_BATCH_SIZE) initialSnapshotReady.complete(Unit)
            }
        }
        val selected = selectPackages(scan.packages)
        val previouslyVerified = synchronized(stateLock) { verifiedPackages.toMap() }
        val updates = replaceState(selected.values, previouslyVerified)
        initialized = true
        initialSnapshotReady.complete(Unit)
        persistReplacementLocked(selected.values)
        updates.forEach(_packageUpdates::tryEmit)
        _changes.tryEmit(Unit)
        BookDownloadCacheRefresh(
            packageCount = selected.size,
            invalidPackageCount = scan.invalidPackageCount,
            duplicatePackageCount = scan.packages.size - selected.size,
            cleanedTemporaryPackageCount = scan.cleanedTemporaryPackageCount,
        )
    }

    private suspend fun verifyFilesystemLocked(reportInitialization: Boolean): BookDownloadCacheRefresh =
        withContext(Dispatchers.IO) {
            if (reportInitialization) _isInitializing.value = true
            try {
                val scan = provider.rebuildPackages()
                val selected = selectVerifiedPackages(scan.packages)
                val indexed = selected.mapValues { (_, download) -> IndexedBookDownloadPackage.from(download) }
                val updates = replaceState(indexed.values, selected)
                initialized = true
                initialSnapshotReady.complete(Unit)
                persistReplacementLocked(indexed.values)
                updates.forEach(_packageUpdates::tryEmit)
                _changes.tryEmit(Unit)
                BookDownloadCacheRefresh(
                    packageCount = indexed.size,
                    invalidPackageCount = scan.invalidPackageCount,
                    duplicatePackageCount = scan.packages.size - selected.size,
                    cleanedTemporaryPackageCount = scan.cleanedTemporaryPackageCount,
                )
            } finally {
                if (reportInitialization) _isInitializing.value = false
            }
        }

    fun get(packageKey: BookDownloadPackageKey): VerifiedBookDownloadPackage? = synchronized(stateLock) {
        verifiedPackages[packageKey]
    }

    fun packageDirectory(packageKey: BookDownloadPackageKey) = synchronized(stateLock) {
        verifiedPackages[packageKey]?.directory ?: packages[packageKey]?.let { indexed ->
            indexStore?.resolveDirectory(indexed)
        }
    }

    fun packagesSnapshot(): List<IndexedBookDownloadPackage> = synchronized(stateLock) { packages.values.toList() }

    suspend fun getVerified(packageKey: BookDownloadPackageKey): VerifiedBookDownloadPackage? {
        synchronized(stateLock) { verifiedPackages[packageKey] }?.let { return it }
        var indexed = synchronized(stateLock) { packages[packageKey] }
        if (indexed == null) {
            initialSnapshotReady.await()
            indexed = synchronized(stateLock) { packages[packageKey] }
            indexed ?: return null
        }

        val verificationLock = packageVerificationLocks.getOrPut(packageKey) { Mutex() }
        return try {
            verificationLock.withLock {
                synchronized(stateLock) { verifiedPackages[packageKey] }?.let { return@withLock it }
                val current = synchronized(stateLock) { packages[packageKey] } ?: return@withLock null
                val directory = indexStore?.resolveDirectory(current) ?: return@withLock null
                val verified = withContext(Dispatchers.IO) {
                    provider.readVerifiedPackage(directory)
                }?.takeIf {
                    it.manifest.packageKey == current.manifest.packageKey &&
                        it.manifest.createdAt == current.manifest.createdAt
                }
                if (verified == null) {
                    remove(listOf(packageKey))
                    null
                } else {
                    synchronized(stateLock) { verifiedPackages[packageKey] = verified }
                    verified
                }
            }
        } finally {
            packageVerificationLocks.remove(packageKey, verificationLock)
        }
    }

    suspend fun findVerifiedOnDisk(entry: Entry, child: EntryChapter): VerifiedBookDownloadPackage? {
        val packageKey = BookDownloadPackageKey(entry.source, entry.url, child.url)
        synchronized(stateLock) { verifiedPackages[packageKey] }?.let { return it }
        val verificationLock = packageVerificationLocks.getOrPut(packageKey) { Mutex() }
        return try {
            verificationLock.withLock {
                synchronized(stateLock) { verifiedPackages[packageKey] }?.let { return@withLock it }
                val verified = withContext(Dispatchers.IO) { provider.findVerifiedPackage(entry, child) }
                    ?: return@withLock null
                val indexed = IndexedBookDownloadPackage.from(verified)
                synchronized(stateLock) { putStateLocked(indexed, verified) }
                if (initialized) {
                    persistMutation { it.upsert(provider.downloadsRootUri(), indexed) }
                }
                _packageUpdates.emit(BookDownloadPackageUpdate(indexed.manifest, downloaded = true))
                verified
            }
        } finally {
            packageVerificationLocks.remove(packageKey, verificationLock)
        }
    }

    suspend fun upsert(download: VerifiedBookDownloadPackage) {
        ensureInitialized()
        val indexed = IndexedBookDownloadPackage.from(download)
        refreshMutex.withLock {
            synchronized(stateLock) { putStateLocked(indexed, download) }
            persistMutation { store ->
                store.upsert(provider.downloadsRootUri(), indexed)
                if (store.shouldCompact()) {
                    store.replace(provider.downloadsRootUri(), packagesSnapshot())
                }
            }
        }
        _packageUpdates.emit(
            BookDownloadPackageUpdate(BookDownloadIndexManifest.from(download.manifest), downloaded = true),
        )
    }

    suspend fun remove(packageKeys: Collection<BookDownloadPackageKey>) {
        if (packageKeys.isEmpty()) return
        ensureInitialized()
        val removed = refreshMutex.withLock {
            val removedPackages = synchronized(stateLock) {
                packageKeys.mapNotNull { removeStateLocked(it) }
            }
            persistMutation { it.remove(provider.downloadsRootUri(), packageKeys) }
            removedPackages
        }
        removed.forEach { _packageUpdates.emit(BookDownloadPackageUpdate(it.manifest, downloaded = false)) }
    }

    suspend fun replace(
        packageKeys: Collection<BookDownloadPackageKey>,
        downloads: Collection<VerifiedBookDownloadPackage>,
    ) {
        ensureInitialized()
        val replacements = selectVerifiedPackages(downloads)
        val updates = refreshMutex.withLock {
            val removed = synchronized(stateLock) {
                packageKeys.mapNotNull { removeStateLocked(it) }
            }
            replacements.values.forEach { download ->
                synchronized(stateLock) { putStateLocked(IndexedBookDownloadPackage.from(download), download) }
            }
            persistMutation { store ->
                store.remove(provider.downloadsRootUri(), packageKeys)
                replacements.values.forEach {
                    store.upsert(provider.downloadsRootUri(), IndexedBookDownloadPackage.from(it))
                }
            }
            removed.map { BookDownloadPackageUpdate(it.manifest, downloaded = false) } +
                replacements.values.map {
                    BookDownloadPackageUpdate(BookDownloadIndexManifest.from(it.manifest), downloaded = true)
                }
        }
        updates.forEach { _packageUpdates.emit(it) }
    }

    fun find(sourceId: Long, childUrl: String, entryTitle: String): IndexedBookDownloadPackage? =
        synchronized(stateLock) {
            val candidates = packageKeysByChild[BookDownloadChildKey(sourceId, childUrl)].orEmpty()
                .mapNotNull(packages::get)
            candidates.singleOrNull() ?: candidates.firstOrNull { it.manifest.entryTitle == entryTitle }
        }

    fun isDownloaded(packageKey: BookDownloadPackageKey): Boolean = synchronized(stateLock) {
        packageKey in packages
    }

    fun getDownloadCount(sourceId: Long, entryUrl: String): Int = synchronized(stateLock) {
        packageKeysByEntryIdentity[BookDownloadEntryKey(sourceId, entryUrl)].orEmpty().size
    }

    fun getDownloadCount(entry: Entry): Int = synchronized(stateLock) {
        val identityMatches = packageKeysByEntryIdentity[BookDownloadEntryKey(entry.source, entry.url)].orEmpty()
        val idMatches = packageKeysByEntryId[entry.id].orEmpty()
        identityMatches.size + idMatches.count { it !in identityMatches }
    }

    fun getTotalDownloadCount(): Int = synchronized(stateLock) { packages.size }

    private fun putStateLocked(
        indexed: IndexedBookDownloadPackage,
        verified: VerifiedBookDownloadPackage?,
    ) {
        val packageKey = indexed.manifest.packageKey
        removeStateLocked(packageKey)
        packages[packageKey] = indexed
        verified?.let { verifiedPackages[packageKey] = it }
        packageKeysByChild.getOrPut(
            BookDownloadChildKey(indexed.manifest.sourceId, indexed.manifest.childUrl),
            ::linkedSetOf,
        ) += packageKey
        packageKeysByEntryIdentity.getOrPut(
            BookDownloadEntryKey(indexed.manifest.sourceId, indexed.manifest.entryUrl),
            ::linkedSetOf,
        ) += packageKey
        packageKeysByEntryId.getOrPut(indexed.manifest.entryId, ::linkedSetOf) += packageKey
    }

    private fun removeStateLocked(packageKey: BookDownloadPackageKey): IndexedBookDownloadPackage? {
        val removed = packages.remove(packageKey) ?: return null
        verifiedPackages.remove(packageKey)
        packageKeysByChild.removeKey(
            BookDownloadChildKey(removed.manifest.sourceId, removed.manifest.childUrl),
            packageKey,
        )
        packageKeysByEntryIdentity.removeKey(
            BookDownloadEntryKey(removed.manifest.sourceId, removed.manifest.entryUrl),
            packageKey,
        )
        packageKeysByEntryId.removeKey(removed.manifest.entryId, packageKey)
        return removed
    }

    private fun replaceState(
        replacements: Collection<IndexedBookDownloadPackage>,
        verified: Map<BookDownloadPackageKey, VerifiedBookDownloadPackage>,
    ): List<BookDownloadPackageUpdate> = synchronized(stateLock) {
        val previous = packages.toMap()
        packages.clear()
        verifiedPackages.clear()
        packageKeysByChild.clear()
        packageKeysByEntryIdentity.clear()
        packageKeysByEntryId.clear()
        replacements.forEach { putStateLocked(it, verified[it.manifest.packageKey]) }

        val removed = previous.keys.minus(packages.keys).map { key ->
            BookDownloadPackageUpdate(checkNotNull(previous[key]).manifest, downloaded = false)
        }
        val added = packages.keys.minus(previous.keys).map { key ->
            BookDownloadPackageUpdate(checkNotNull(packages[key]).manifest, downloaded = true)
        }
        removed + added
    }

    private fun selectPackages(
        downloads: Collection<IndexedBookDownloadPackage>,
    ): Map<BookDownloadPackageKey, IndexedBookDownloadPackage> = downloads
        .groupBy { it.manifest.packageKey }
        .mapValues { (_, packages) -> packages.maxWith(compareBy { it.manifest.createdAt }) }

    private fun selectVerifiedPackages(
        downloads: Collection<VerifiedBookDownloadPackage>,
    ): Map<BookDownloadPackageKey, VerifiedBookDownloadPackage> = downloads
        .groupBy { it.manifest.packageKey }
        .mapValues { (_, packages) ->
            packages.maxWith(
                compareBy<VerifiedBookDownloadPackage> { it.manifest.createdAt }
                    .thenBy { it.directory.uri.toString() },
            )
        }

    private suspend fun persistReplacementLocked(packages: Collection<IndexedBookDownloadPackage>) {
        persistMutation { it.replace(provider.downloadsRootUri(), packages) }
    }

    private suspend fun persistMutation(mutation: (BookDownloadIndexStore) -> Unit) = withContext(Dispatchers.IO) {
        val store = indexStore ?: return@withContext
        runCatching { mutation(store) }
            .onFailure { error -> logcat(LogPriority.ERROR, error) { "Failed to persist BOOK download index" } }
    }

    private fun IndexedBookDownloadPackage.isNewerThan(other: IndexedBookDownloadPackage): Boolean =
        manifest.createdAt > other.manifest.createdAt ||
            (manifest.createdAt == other.manifest.createdAt && directoryUri > other.directoryUri)

    private fun <Key> MutableMap<Key, MutableSet<BookDownloadPackageKey>>.removeKey(
        indexKey: Key,
        packageKey: BookDownloadPackageKey,
    ) {
        val keys = this[indexKey] ?: return
        keys -= packageKey
        if (keys.isEmpty()) remove(indexKey)
    }

    private companion object {
        const val INITIAL_DISCOVERY_BATCH_SIZE = 32
    }
}

internal data class BookDownloadPackageUpdate(
    val manifest: BookDownloadIndexManifest,
    val downloaded: Boolean,
)

internal data class BookDownloadCacheRefresh(
    val packageCount: Int,
    val invalidPackageCount: Int,
    val duplicatePackageCount: Int,
    val cleanedTemporaryPackageCount: Int,
)

private data class BookDownloadChildKey(
    val sourceId: Long,
    val childUrl: String,
)

private data class BookDownloadEntryKey(
    val sourceId: Long,
    val entryUrl: String,
)
