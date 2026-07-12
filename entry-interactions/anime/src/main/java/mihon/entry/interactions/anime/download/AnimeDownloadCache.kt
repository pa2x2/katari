package mihon.entry.interactions.anime.download
import android.app.Application
import android.content.Context
import androidx.core.net.toUri
import com.hippo.unifile.UniFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.protobuf.ProtoBuf
import logcat.LogPriority
import mihon.entry.interactions.anime.download.AnimeDownloadManager.Companion.TMP_DIR_SUFFIX
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.storage.service.StorageManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalSerializationApi::class)
internal class AnimeDownloadCache(
    private val context: Context,
    private val provider: AnimeDownloadProvider = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val storageManager: StorageManager = Injekt.get(),
) {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _changes: Channel<Unit> = Channel(Channel.UNLIMITED)
    val changes = _changes.receiveAsFlow()
        .onStart { emit(Unit) }
        .shareIn(scope, SharingStarted.Lazily, 1)

    private val renewInterval = 1.hours.inWholeMilliseconds
    private var lastRenew = 0L
    private var renewalJob: Job? = null

    private val _isInitializing = MutableStateFlow(false)
    val isInitializing = _isInitializing
        .debounce(1000L)
        .stateIn(scope, SharingStarted.WhileSubscribed(), false)

    private val diskCacheFile: File
        get() = File(context.cacheDir, "anime_dl_index_cache_v1")

    private val rootDownloadsDirMutex = Mutex()
    private var rootDownloadsDir = AnimeRootDirectory(storageManager.getDownloadsDirectory())

    init {
        scope.launch {
            rootDownloadsDirMutex.withLock {
                try {
                    if (diskCacheFile.exists()) {
                        rootDownloadsDir = ProtoBuf.decodeFromByteArray(diskCacheFile.readBytes())
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

    fun getTotalDownloadCount(): Int {
        renewCache()
        return rootDownloadsDir.sourceDirs.values.sumOf { sourceDir ->
            sourceDir.animeDirs.values.sumOf { animeDir -> animeDir.episodeDirs.size }
        }
    }

    fun getDownloadCount(anime: Entry): Int {
        renewCache()
        val sourceDir = rootDownloadsDir.sourceDirs[anime.source] ?: return 0
        val animeDir = sourceDir.animeDirs[provider.getAnimeDirName(anime.title)] ?: return 0
        return animeDir.episodeDirs.size
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

            val animeDirName = provider.getAnimeDirName(anime.title)
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
            val animeDir = sourceDir.animeDirs[provider.getAnimeDirName(anime.title)] ?: return
            episodes.forEach { episode ->
                provider.getValidEpisodeDirNames(episode.name, episode.url).forEach {
                    animeDir.episodeDirs -= it
                }
            }
        }

        notifyChanges()
    }

    suspend fun removeAnime(anime: Entry) {
        rootDownloadsDirMutex.withLock {
            val sourceDir = rootDownloadsDir.sourceDirs[anime.source] ?: return
            sourceDir.animeDirs -= provider.getAnimeDirName(anime.title)
        }

        notifyChanges()
    }

    fun invalidateCache() {
        lastRenew = 0L
        renewalJob?.cancel()
        diskCacheFile.delete()
        renewCache()
    }

    private fun renewCache() {
        if (lastRenew + renewInterval >= System.currentTimeMillis() || renewalJob?.isActive == true) {
            return
        }

        renewalJob = scope.launchIO {
            if (lastRenew == 0L) {
                _isInitializing.emit(true)
            }

            var sources = emptyList<Pair<Long, String>>()
            withTimeoutOrNull(30.seconds) {
                sourceManager.isInitialized.first { it }
                sources =
                    sourceManager.getCatalogueSources().map {
                        it.id to
                            provider.getSourceDirName(it)
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
                            animeDir.episodeDirs = animeDir.dir?.listFiles().orEmpty()
                                .mapNotNull {
                                    when {
                                        it.name?.endsWith(TMP_DIR_SUFFIX) == true -> null
                                        it.isDirectory && !it.name.isNullOrBlank() -> it.name
                                        else -> null
                                    }
                                }
                                .toMutableSet()
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
                lastRenew = System.currentTimeMillis()
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
            ensureActive()
            val bytes = ProtoBuf.encodeToByteArray(rootDownloadsDir)
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
    @Serializable(with = AnimeUniFileAsStringSerializer::class)
    val dir: UniFile?,
    var sourceDirs: Map<Long, AnimeSourceDirectory> = mapOf(),
)

@Serializable
private class AnimeSourceDirectory(
    @Serializable(with = AnimeUniFileAsStringSerializer::class)
    val dir: UniFile?,
    var animeDirs: Map<String, AnimeTitleDirectory> = mapOf(),
)

@Serializable
private class AnimeTitleDirectory(
    @Serializable(with = AnimeUniFileAsStringSerializer::class)
    val dir: UniFile?,
    var episodeDirs: MutableSet<String> = mutableSetOf(),
)

@OptIn(ExperimentalSerializationApi::class)
private object AnimeUniFileAsStringSerializer : KSerializer<UniFile?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UniFile", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UniFile?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeString(value.uri.toString())
        }
    }

    override fun deserialize(decoder: Decoder): UniFile? {
        return if (decoder.decodeNotNullMark()) {
            UniFile.fromUri(Injekt.get<Application>(), decoder.decodeString().toUri())
        } else {
            decoder.decodeNull()
        }
    }
}
