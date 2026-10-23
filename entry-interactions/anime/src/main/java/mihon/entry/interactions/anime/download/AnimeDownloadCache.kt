package mihon.entry.interactions.anime.download
import android.content.Context
import androidx.core.net.toUri
import androidx.core.util.AtomicFile
import com.hippo.unifile.UniFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
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
import mihon.entry.interactions.anime.download.AnimeDownloadManager.Companion.TMP_DIR_SUFFIX
import mihon.entry.interactions.catalogue.EntryCatalogueFeature
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.storage.service.StorageManager
import java.io.File
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalSerializationApi::class)
internal class AnimeDownloadCache(
    private val context: Context,
    private val provider: AnimeDownloadProvider,
    private val sourceManager: SourceManager,
    private val catalogueFeature: () -> EntryCatalogueFeature,
    private val storageManager: StorageManager,
    private val scope: CoroutineScope,
) {

    private val protoBuf = ProtoBuf {
        serializersModule = SerializersModule { contextual(UniFile::class, AnimeUniFileAsStringSerializer(context)) }
    }

    private val _changes: Channel<Unit> = Channel(Channel.UNLIMITED)
    val changes = _changes.receiveAsFlow()
        .onStart { emit(Unit) }
        .shareIn(scope, SharingStarted.Lazily, 1)

    private val renewInterval = 1.hours.inWholeMilliseconds

    @Volatile
    private var lastRenew = 0L
    private var renewalJob: Job? = null
    private val initJob: Job

    private val _isInitializing = MutableStateFlow(false)
    val isInitializing = _isInitializing
        .debounce(1000L)
        .stateIn(scope, SharingStarted.WhileSubscribed(), false)

    private val diskCacheFile: AtomicFile
        get() = AtomicFile(File(context.cacheDir, "anime_dl_index_cache_v1"))

    private val rootDownloadsDirMutex = Mutex()

    @Volatile
    private var rootDownloadsDir = AnimeRootDirectory(storageManager.getDownloadsDirectory())

    init {
        initJob = scope.launch {
            rootDownloadsDirMutex.withLock {
                try {
                    if (diskCacheFile.baseFile.exists()) {
                        rootDownloadsDir = protoBuf.decodeFromByteArray(diskCacheFile.readFully())
                        lastRenew = System.currentTimeMillis()
                    }
                } catch (e: Throwable) {
                    logcat(LogPriority.ERROR, e) { "Failed to initialize anime download cache from disk cache" }
                    diskCacheFile.delete()
                }
            }
        }

        storageManager.changes
            .onEach { invalidateCache() }
            .launchIn(scope)
    }

    fun isEpisodeDownloaded(
        episodeName: String,
        episodeUrl: String,
        animeTitle: String,
        sourceId: Long,
        skipCache: Boolean = false,
    ): Boolean {
        if (skipCache) {
            val source = sourceManager.get(sourceId) ?: return false
            return provider.findEpisodeDir(episodeName, episodeUrl, animeTitle, source) != null
        }

        renewCache()

        val sourceDir = rootDownloadsDir.sourceDirs[sourceId] ?: return false
        val animeDir = sourceDir.animeDirs[provider.getAnimeDirName(animeTitle)] ?: return false
        return provider.getValidEpisodeDirNames(episodeName, episodeUrl).any { it in animeDir.episodeDirs }
    }

    /** Returns the ids of the downloaded episodes among [episodes], all of the anime titled [animeTitle]. */
    fun getDownloadedEpisodeIds(episodes: List<EntryChapter>, animeTitle: String, sourceId: Long): Set<Long> {
        renewCache()

        val animeDir = rootDownloadsDir.sourceDirs[sourceId]?.animeDirs?.get(provider.getAnimeDirName(animeTitle))
        val episodeDirs = animeDir?.episodeDirs
        if (episodeDirs.isNullOrEmpty()) return emptySet()

        return episodes
            .filter { episode -> provider.getValidEpisodeDirNames(episode.name, episode.url).any { it in episodeDirs } }
            .mapTo(HashSet()) { it.id }
    }

    fun getTotalDownloadCount(): Int {
        renewCache()
        return rootDownloadsDir.sourceDirs.values.sumOf { sourceDir ->
            sourceDir.animeDirs.values.sumOf { animeDir -> animeDir.episodeDirs.size }
        }
    }

    fun getDownloadCount(anime: Entry): Int {
        renewCache()
        val sourceDir = rootDownloadsDir.sourceDirs[anime.source] ?: return 0
        val animeDir = sourceDir.animeDirs[provider.getAnimeDirName(anime.downloadFolderTitle)] ?: return 0
        return animeDir.episodeDirs.size
    }

    fun getDownloadCounts(animes: List<Entry>): Map<Long, Int> {
        renewCache()
        val sourceDirs = rootDownloadsDir.sourceDirs
        return animes.associate { anime ->
            val animeDirs = sourceDirs[anime.source]?.animeDirs
            // Building the directory name is the costly part, so it's skipped for sources without downloads
            val animeDir = if (animeDirs.isNullOrEmpty()) {
                null
            } else {
                animeDirs[
                    provider.getAnimeDirName(
                        anime.downloadFolderTitle,
                    ),
                ]
            }
            anime.id to (animeDir?.episodeDirs?.size ?: 0)
        }
    }

    suspend fun addEpisode(episodeDirName: String, animeUniFile: UniFile, anime: Entry) {
        rootDownloadsDirMutex.withLock {
            var sourceDir = rootDownloadsDir.sourceDirs[anime.source]
            if (sourceDir == null) {
                val source = sourceManager.get(anime.source) ?: return
                val sourceUniFile = provider.findSourceDir(source) ?: return
                sourceDir = AnimeSourceDirectory(sourceUniFile)
                rootDownloadsDir.sourceDirs += anime.source to sourceDir
            }

            val animeDirName = provider.getAnimeDirName(anime.downloadFolderTitle)
            var animeDir = sourceDir.animeDirs[animeDirName]
            if (animeDir == null) {
                animeDir = AnimeTitleDirectory(animeUniFile)
                sourceDir.animeDirs += animeDirName to animeDir
            }

            animeDir.episodeDirs += episodeDirName
        }

        notifyChanges()
    }

    suspend fun removeEpisodes(episodes: List<EntryChapter>, anime: Entry) {
        rootDownloadsDirMutex.withLock {
            val sourceDir = rootDownloadsDir.sourceDirs[anime.source] ?: return
            val animeDir = sourceDir.animeDirs[provider.getAnimeDirName(anime.downloadFolderTitle)] ?: return
            animeDir.episodeDirs -= episodes
                .flatMap { provider.getValidEpisodeDirNames(it.name, it.url) }
                .toSet()
        }

        notifyChanges()
    }

    suspend fun removeAnime(anime: Entry) {
        rootDownloadsDirMutex.withLock {
            val sourceDir = rootDownloadsDir.sourceDirs[anime.source] ?: return
            sourceDir.animeDirs -= provider.getAnimeDirName(anime.downloadFolderTitle)
        }

        notifyChanges()
    }

    fun invalidateCache() {
        lastRenew = 0L
        renewalJob?.cancel()
        renewCache(deleteDiskCache = true)
    }

    private fun renewCache(deleteDiskCache: Boolean = false) {
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

            var sources = emptyList<Pair<Long, String>>()
            withTimeoutOrNull(30.seconds) {
                catalogueFeature().isInitialized.first { it }
                sources =
                    catalogueFeature().sources().map {
                        it.id to
                            provider.getSourceDirName(it.name)
                    }
            }

            val sourceMap = sources.associate { it.second.lowercase() to it.first }

            rootDownloadsDirMutex.withLock {
                val updatedRootDir = AnimeRootDirectory(storageManager.getDownloadsDirectory())
                updatedRootDir.sourceDirs = updatedRootDir.dir?.listFiles().orEmpty()
                    .filter { it.isDirectory && !it.name.isNullOrBlank() }
                    .mapNotNull { dir ->
                        val sourceId = sourceMap[dir.name!!.lowercase()]
                        sourceId?.let { it to AnimeSourceDirectory(dir) }
                    }
                    .toMap()

                updatedRootDir.sourceDirs.values.map { sourceDir ->
                    async {
                        sourceDir.animeDirs = sourceDir.dir?.listFiles().orEmpty()
                            .filter { it.isDirectory && !it.name.isNullOrBlank() }
                            .associate { it.name!! to AnimeTitleDirectory(it) }

                        sourceDir.animeDirs.values.forEach { animeDir ->
                            animeDir.dir?.let(provider::recoverEpisodePackages)
                            animeDir.episodeDirs = animeDir.dir?.listFiles().orEmpty()
                                .mapNotNull {
                                    when {
                                        it.name?.endsWith(TMP_DIR_SUFFIX) == true -> null
                                        it.name?.endsWith(AnimeDownloadProvider.BACKUP_DIR_SUFFIX) == true -> null
                                        it.isDirectory &&
                                            !it.name.isNullOrBlank() &&
                                            provider.isEpisodePackageValid(it) -> it.name
                                        else -> null
                                    }
                                }
                                .toSet()
                        }
                    }
                }.awaitAll()

                rootDownloadsDir = updatedRootDir
            }

            _isInitializing.emit(false)
        }.also {
            it.invokeOnCompletion { exception ->
                if (exception != null && exception !is CancellationException) {
                    logcat(LogPriority.ERROR, exception) { "AnimeDownloadCache: failed to create cache" }
                }
                // A cancelled renewal (e.g. from invalidateCache) must not hold off the one replacing it
                if (exception !is CancellationException) lastRenew = System.currentTimeMillis()
                notifyChanges()
            }
        }

        notifyChanges()
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
            delay(1000)
            val bytes = rootDownloadsDirMutex.withLock {
                protoBuf.encodeToByteArray(rootDownloadsDir)
            }
            ensureActive()
            try {
                diskCacheFile.writeBytes(bytes)
            } catch (e: Throwable) {
                logcat(LogPriority.ERROR, e) { "Failed to write anime download disk cache file" }
            }
        }
    }
}

@Serializable
private class AnimeRootDirectory(
    @Contextual
    val dir: UniFile?,
    @Volatile
    var sourceDirs: Map<Long, AnimeSourceDirectory> = mapOf(),
)

@Serializable
private class AnimeSourceDirectory(
    @Contextual
    val dir: UniFile?,
    @Volatile
    var animeDirs: Map<String, AnimeTitleDirectory> = mapOf(),
)

@Serializable
private class AnimeTitleDirectory(
    @Contextual
    val dir: UniFile?,
    // Replaced rather than changed in place, so the lock-free lookups always read a consistent set
    @Volatile
    var episodeDirs: Set<String> = emptySet(),
)

private class AnimeUniFileAsStringSerializer(private val context: Context) : KSerializer<UniFile> {
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
