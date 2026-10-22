package mihon.entry.interactions.manga.download
import android.content.Context
import androidx.core.net.toUri
import androidx.core.util.AtomicFile
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.protobuf.ProtoBuf
import logcat.LogPriority
import tachiyomi.core.common.storage.extension
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.storage.service.StorageManager
import tachiyomi.source.local.LocalSource
import java.io.File
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

/**
 * Cache where we dump the downloads directory from the filesystem. This class is needed because
 * directory checking is expensive and it slows down the app. The cache is invalidated by the time
 * defined in [renewInterval] as we don't have any control over the filesystem and the user can
 * delete the folders at any time without the app noticing.
 */
@OptIn(ExperimentalSerializationApi::class)
internal class DownloadCache(
    private val context: Context,
    private val provider: DownloadProvider,
    private val sourceManager: SourceManager,
    private val storageManager: StorageManager,
    private val scope: CoroutineScope,
) {

    private val protoBuf = ProtoBuf {
        serializersModule = SerializersModule { contextual(UniFile::class, UniFileAsStringSerializer(context)) }
    }

    private val _changes: Channel<Unit> = Channel(Channel.UNLIMITED)
    val changes = _changes.receiveAsFlow()
        .onStart { emit(Unit) }
        .shareIn(scope, SharingStarted.Lazily, 1)

    /**
     * The interval after which this cache should be invalidated. 1 hour shouldn't cause major
     * issues, as the cache is only used for UI feedback.
     */
    private val renewInterval = 1.hours.inWholeMilliseconds

    /**
     * The last time the cache was refreshed.
     */
    @Volatile
    private var lastRenew = 0L
    private var renewalJob: Job? = null
    private val initJob: Job

    private val _isInitializing = MutableStateFlow(false)
    val isInitializing = _isInitializing
        .debounce(1.seconds) // Don't notify if it finishes quickly enough
        .stateIn(scope, SharingStarted.WhileSubscribed(), false)

    private val diskCacheFile: AtomicFile
        get() = AtomicFile(File(context.cacheDir, "dl_index_cache_v3"))

    private val rootDownloadsDirMutex = Mutex()

    @Volatile
    private var rootDownloadsDir = RootDirectory(storageManager.getDownloadsDirectory())

    init {
        // Attempt to read cache file
        initJob = scope.launch {
            rootDownloadsDirMutex.withLock {
                try {
                    if (diskCacheFile.baseFile.exists()) {
                        val diskCache = diskCacheFile.openRead().use {
                            protoBuf.decodeFromByteArray<RootDirectory>(it.readBytes())
                        }
                        rootDownloadsDir = diskCache
                        lastRenew = System.currentTimeMillis()
                    }
                } catch (e: Throwable) {
                    logcat(LogPriority.ERROR, e) { "Failed to initialize from disk cache" }
                    diskCacheFile.delete()
                }
            }
        }

        storageManager.changes
            .onEach { invalidateCache() }
            .launchIn(scope)
    }

    /**
     * Returns true if the chapter is downloaded.
     *
     * @param chapterName the name of the chapter to query.
     * @param chapterScanlator scanlator of the chapter to query
     * @param chapterUrl the url of the chapter to query
     * @param mangaTitle the title of the manga to query.
     * @param sourceId the id of the source of the chapter.
     * @param skipCache whether to skip the directory cache and check in the filesystem.
     */
    fun isChapterDownloaded(
        chapterName: String,
        chapterScanlator: String?,
        chapterUrl: String,
        mangaTitle: String,
        sourceId: Long,
        skipCache: Boolean,
    ): Boolean {
        if (skipCache) {
            val source = sourceManager.getOrStub(sourceId)
            return provider.findChapterDir(chapterName, chapterScanlator, chapterUrl, mangaTitle, source) != null
        }

        renewCache()

        val sourceDir = rootDownloadsDir.sourceDirs[sourceId]
        if (sourceDir != null) {
            val mangaDir = sourceDir.mangaDirs[provider.getEntryDirName(mangaTitle)]
            if (mangaDir != null) {
                return provider.getValidChapterDirNames(
                    chapterName,
                    chapterScanlator,
                    chapterUrl,
                ).any { it in mangaDir.chapterDirs }
            }
        }
        return false
    }

    /** Returns the ids of the downloaded chapters among [chapters], all of the manga titled [mangaTitle]. */
    fun getDownloadedChapterIds(chapters: List<EntryChapter>, mangaTitle: String, sourceId: Long): Set<Long> {
        renewCache()

        val mangaDir = rootDownloadsDir.sourceDirs[sourceId]?.mangaDirs?.get(provider.getEntryDirName(mangaTitle))
        val chapterDirs = mangaDir?.chapterDirs
        if (chapterDirs.isNullOrEmpty()) return emptySet()

        return chapters
            .filter { chapter ->
                provider.getValidChapterDirNames(chapter.name, chapter.scanlator, chapter.url).any { it in chapterDirs }
            }
            .mapTo(HashSet()) { it.id }
    }

    /**
     * Returns the amount of downloaded chapters.
     */
    fun getTotalDownloadCount(): Int {
        renewCache()

        return rootDownloadsDir.sourceDirs.values.sumOf { sourceDir ->
            sourceDir.mangaDirs.values.sumOf { mangaDir ->
                mangaDir.chapterDirs.size
            }
        }
    }

    /**
     * Returns the amount of downloaded chapters for a manga.
     *
     * @param manga the manga to check.
     */
    fun getDownloadCount(manga: Entry): Int {
        renewCache()

        val sourceDir = rootDownloadsDir.sourceDirs[manga.source]
        if (sourceDir != null) {
            val mangaDir = sourceDir.mangaDirs[provider.getEntryDirName(manga.title)]
            if (mangaDir != null) {
                return mangaDir.chapterDirs.size
            }
        }
        return 0
    }

    /** Returns the amount of downloaded chapters of each of [mangas], by entry id. */
    fun getDownloadCounts(mangas: List<Entry>): Map<Long, Int> {
        renewCache()

        val sourceDirs = rootDownloadsDir.sourceDirs
        return mangas.associate { manga ->
            val mangaDirs = sourceDirs[manga.source]?.mangaDirs
            // Building the directory name is the costly part, so it's skipped for sources without downloads
            val mangaDir = if (mangaDirs.isNullOrEmpty()) null else mangaDirs[provider.getEntryDirName(manga.title)]
            manga.id to (mangaDir?.chapterDirs?.size ?: 0)
        }
    }

    /**
     * Adds a chapter that has just been downloaded to this cache.
     *
     * @param chapterDirName the downloaded chapter's directory name.
     * @param mangaUniFile the directory of the manga.
     * @param manga the manga of the chapter.
     */
    suspend fun addChapter(chapterDirName: String, mangaUniFile: UniFile, manga: Entry) {
        addChapters(setOf(chapterDirName), mangaUniFile, manga)
    }

    /**
     * Adds downloaded chapters of [manga], found in [mangaUniFile] under [chapterDirNames], to this cache. Returns
     * once the index on disk includes them.
     */
    suspend fun addChapters(chapterDirNames: Set<String>, mangaUniFile: UniFile, manga: Entry) {
        if (chapterDirNames.isEmpty()) return
        commit {
            // Retrieve the cached source directory or cache a new one
            var sourceDir = rootDownloadsDir.sourceDirs[manga.source]
            if (sourceDir == null) {
                val source = sourceManager.get(manga.source) ?: return@commit false
                val sourceUniFile = provider.findSourceDirs(source).firstOrNull() ?: return@commit false
                sourceDir = SourceDirectory(sourceUniFile)
                rootDownloadsDir.sourceDirs += manga.source to sourceDir
            }

            // Retrieve the cached manga directory or cache a new one
            val mangaDirName = provider.getEntryDirName(manga.title)
            var mangaDir = sourceDir.mangaDirs[mangaDirName]
            if (mangaDir == null) {
                mangaDir = MangaDirectory(mangaUniFile)
                sourceDir.mangaDirs += mangaDirName to mangaDir
            }

            if (mangaDir.chapterDirs.containsAll(chapterDirNames)) return@commit false
            mangaDir.chapterDirs += chapterDirNames
            true
        }
    }

    /**
     * Removes a list of chapters that have been deleted from this cache.
     *
     * @param chapters the list of chapter to remove.
     * @param manga the manga of the chapter.
     */
    suspend fun removeChapters(chapters: List<EntryChapter>, manga: Entry) {
        commit {
            val sourceDir = rootDownloadsDir.sourceDirs[manga.source] ?: return@commit false
            val mangaDir = sourceDir.mangaDirs[provider.getEntryDirName(manga.title)] ?: return@commit false
            mangaDir.chapterDirs -= chapters
                .flatMap { provider.getValidChapterDirNames(it.name, it.scanlator, it.url) }
                .toSet()
            true
        }
    }

    /**
     * Removes a manga that has been deleted from this cache.
     *
     * @param manga the manga to remove.
     */
    suspend fun removeManga(manga: Entry) {
        commit {
            val sourceDir = rootDownloadsDir.sourceDirs[manga.source] ?: return@commit false
            val mangaDirName = provider.getEntryDirName(manga.title)
            if (!sourceDir.mangaDirs.containsKey(mangaDirName)) return@commit false
            sourceDir.mangaDirs -= mangaDirName
            true
        }
    }

    /**
     * Renames a manga in this cache.
     *
     * @param manga the manga being renamed.
     * @param mangaUniFile the manga's new directory.
     * @param newTitle the manga's new title.
     */
    suspend fun renameManga(manga: Entry, mangaUniFile: UniFile, newTitle: String) {
        commit {
            val sourceDir = rootDownloadsDir.sourceDirs[manga.source] ?: return@commit false
            val oldMangaDirName = provider.getEntryDirName(manga.title)
            var oldChapterDirs: Set<String>? = null
            // Save the old name's cached chapter dirs
            if (sourceDir.mangaDirs.containsKey(oldMangaDirName)) {
                oldChapterDirs = sourceDir.mangaDirs[oldMangaDirName]?.chapterDirs
                sourceDir.mangaDirs -= oldMangaDirName
            }

            // Retrieve/create the cached manga directory for new name
            val newMangaDirName = provider.getEntryDirName(newTitle)
            var mangaDir = sourceDir.mangaDirs[newMangaDirName]
            if (mangaDir == null) {
                mangaDir = MangaDirectory(mangaUniFile)
                sourceDir.mangaDirs += newMangaDirName to mangaDir
            }

            // Add the old chapters to new name's cache
            if (!oldChapterDirs.isNullOrEmpty()) {
                mangaDir.chapterDirs += oldChapterDirs
            }
            true
        }
    }

    suspend fun removeSource(source: UnifiedSource) {
        commit {
            if (source.id !in rootDownloadsDir.sourceDirs) return@commit false
            rootDownloadsDir.sourceDirs -= source.id
            true
        }
    }

    /**
     * Applies [change] to the cache and, when it reports a change, returns once the index on disk holds it.
     *
     * Each download and deletion is also recorded elsewhere: a download stays in the download queue's store until
     * this returns, a deletion is gone from storage. Waiting for the index, rather than writing it a moment later,
     * leaves no point at which the process can die with the change recorded nowhere, and a new process trusts the
     * index for [renewInterval] without looking at storage.
     */
    private suspend fun commit(change: () -> Boolean) {
        // Read once at startup, the index on disk would otherwise replace a change made before it was read
        initJob.join()
        val changed = rootDownloadsDirMutex.withLock { change() }
        if (!changed) return
        scope.launchNonCancellable { _changes.send(Unit) }
        updateDiskCacheJob?.cancel()
        writeDiskCache()
    }

    fun invalidateCache() {
        lastRenew = 0L
        renewalJob?.cancel()
        renewCache(deleteDiskCache = true)
    }

    /**
     * Renews the downloads cache.
     */
    private fun renewCache(deleteDiskCache: Boolean = false) {
        // Avoid renewing cache if in the process nor too often
        if (lastRenew + renewInterval >= System.currentTimeMillis() || renewalJob?.isActive == true) {
            return
        }

        renewalJob = scope.launchIO {
            // The disk index is older than this scan and would replace it if read afterwards
            initJob.join()
            if (deleteDiskCache) diskCacheFile.delete()

            if (lastRenew == 0L) {
                _isInitializing.emit(true)
            }

            // Try to wait until extensions and sources have loaded
            var sources = emptyList<UnifiedSource>()
            withTimeoutOrNull(30.seconds) {
                sourceManager.isInitialized.first { it }

                sources = getSources()
            }

            val sourceDirNames = sources.associate { it.id to provider.getSourceDirNames(it) }

            rootDownloadsDirMutex.withLock {
                val updatedRootDir = RootDirectory(storageManager.getDownloadsDirectory())

                val folders = updatedRootDir.dir?.listFiles().orEmpty()
                    .filter { it.isDirectory && !it.name.isNullOrBlank() }
                    .associateByTo(sortedMapOf(String.CASE_INSENSITIVE_ORDER)) { it.name!! }
                legacySourceDirRenames(folders.keys, sourceDirNames.values).forEach { (legacyName, currentName) ->
                    if (folders.getValue(legacyName).renameTo(currentName)) {
                        folders.remove(legacyName)
                        updatedRootDir.dir?.findFile(currentName)?.let { folders[currentName] = it }
                    } else {
                        logcat(LogPriority.ERROR) { "Failed to rename legacy source download folder: $legacyName" }
                    }
                }

                val foldersBySource = sourceDirNames
                    .mapValues { (_, names) -> names.all.mapNotNull { folders[it] } }
                    .filterValues { it.isNotEmpty() }

                // A legacy folder shared by several sources is listed once, and they share its entries
                val mangaDirsByFolder = foldersBySource.values.flatten().distinctBy { it.name!!.lowercase() }
                    .map { folder ->
                        async { folder.name!!.lowercase() to listMangaDirs(folder) }
                    }
                    .awaitAll()
                    .toMap()

                updatedRootDir.sourceDirs = foldersBySource.mapValues { (_, sourceFolders) ->
                    // Entries in the current folder take the place of same-named ones in the legacy folder
                    val mangaDirs = sourceFolders.asReversed()
                        .flatMap { mangaDirsByFolder.getValue(it.name!!.lowercase()).toList() }
                        .toMap()
                    SourceDirectory(sourceFolders.first(), mangaDirs)
                }

                rootDownloadsDir = updatedRootDir
            }

            _isInitializing.emit(false)
        }.also {
            it.invokeOnCompletion { exception ->
                if (exception != null && exception !is CancellationException) {
                    logcat(LogPriority.ERROR, exception) { "DownloadCache: failed to create cache" }
                }
                // A cancelled renewal (e.g. from invalidateCache) must not hold off the one replacing it
                if (exception !is CancellationException) lastRenew = System.currentTimeMillis()
                notifyChanges()
            }
        }

        // Mainly to notify the indexing notifier UI
        notifyChanges()
    }

    private fun listMangaDirs(sourceFolder: UniFile): Map<String, MangaDirectory> {
        val mangaDirs = sourceFolder.listFiles().orEmpty()
            .filter { it.isDirectory && !it.name.isNullOrBlank() }
            .associate { it.name!! to MangaDirectory(it) }

        mangaDirs.values.forEach { mangaDir ->
            mangaDir.dir?.recoverMangaPublicationBackups()
            val chapterDirs = mangaDir.dir?.listFiles().orEmpty()
                .mapNotNull {
                    when {
                        // Ignore incomplete downloads
                        it.name?.endsWith(Downloader.TMP_DIR_SUFFIX) == true -> null
                        it.isValidMangaChapterArtifact() -> it.chapterDirName
                        // Anything else is irrelevant
                        else -> null
                    }
                }
                .toSet()

            mangaDir.chapterDirs = chapterDirs
        }
        return mangaDirs
    }

    private fun getSources(): List<UnifiedSource> {
        return sourceManager.getAll().filterNot { it.id == LocalSource.ID } + sourceManager.getStubSources()
    }

    private fun notifyChanges() {
        scope.launchNonCancellable {
            _changes.send(Unit)
        }
        updateDiskCache()
    }

    private var updateDiskCacheJob: Job? = null
    private fun updateDiskCache() {
        updateDiskCacheJob?.cancel()
        updateDiskCacheJob = scope.launchIO {
            delay(1.seconds)
            writeDiskCache()
        }
    }

    // Writes encode the index in the order they take this lock, so a slower earlier write can't replace a later one
    private val diskCacheWriteMutex = Mutex()

    private suspend fun writeDiskCache() = withIOContext {
        diskCacheWriteMutex.withLock {
            val bytes = rootDownloadsDirMutex.withLock {
                protoBuf.encodeToByteArray(rootDownloadsDir)
            }
            try {
                diskCacheFile.writeBytes(bytes)
            } catch (e: Throwable) {
                logcat(
                    priority = LogPriority.ERROR,
                    throwable = e,
                    message = { "Failed to write disk cache file" },
                )
            }
        }
    }
}

/**
 * Class to store the files under the root downloads directory.
 */
@Serializable
private class RootDirectory(
    @Contextual
    val dir: UniFile?,
    @Volatile
    var sourceDirs: Map<Long, SourceDirectory> = mapOf(),
)

/**
 * Class to store the files under a source directory.
 */
@Serializable
private class SourceDirectory(
    @Contextual
    val dir: UniFile?,
    @Volatile
    var mangaDirs: Map<String, MangaDirectory> = mapOf(),
)

/**
 * Class to store the files under a manga directory.
 */
@Serializable
private class MangaDirectory(
    @Contextual
    val dir: UniFile?,
    // Replaced rather than changed in place, so the lock-free lookups always read a consistent set
    @Volatile
    var chapterDirs: Set<String> = emptySet(),
)

private class UniFileAsStringSerializer(private val context: Context) : KSerializer<UniFile> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UniFile", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UniFile) {
        encoder.encodeString(value.uri.toString())
    }

    override fun deserialize(decoder: Decoder): UniFile {
        val uri = decoder.decodeString().toUri()
        return UniFile.fromUri(context, uri) ?: throw SerializationException("Unsupported directory URI: $uri")
    }
}

// Like androidx.core.util.writeBytes, which only takes the platform AtomicFile: the previous index stays whole if the
// write fails
private fun AtomicFile.writeBytes(bytes: ByteArray) {
    val stream = startWrite()
    try {
        stream.write(bytes)
        finishWrite(stream)
    } catch (e: Throwable) {
        failWrite(stream)
        throw e
    }
}
