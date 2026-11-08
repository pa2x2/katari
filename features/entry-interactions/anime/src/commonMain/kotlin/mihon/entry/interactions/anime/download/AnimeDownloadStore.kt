package mihon.entry.interactions.anime.download

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mihon.entry.interactions.anime.download.model.AnimeDownload
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.entry.model.DownloadPreferences
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.VideoDownloadQualityMode
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository

/** Persists queued anime downloads across process restarts. */
internal class AnimeDownloadStore(
    private val backend: AnimeDownloadStoreBackend,
    private val json: Json,
    private val entryRepository: EntryRepository,
    private val entryChapterRepository: EntryChapterRepository,
) {

    constructor(
        preferences: PreferenceStore,
        json: Json,
        entryRepository: EntryRepository,
        entryChapterRepository: EntryChapterRepository,
    ) : this(
        backend = PreferenceAnimeDownloadStoreBackend(preferences),
        json = json,
        entryRepository = entryRepository,
        entryChapterRepository = entryChapterRepository,
    )

    private var nextOrder = backend.values()
        .mapNotNull { (_, value) -> (value as? String)?.let(::deserialize) }
        .maxOfOrNull { it.order + 1 }
        ?: 0

    @Synchronized
    fun addAll(downloads: List<AnimeDownload>) {
        backend.putAll(
            downloads.associate { download ->
                getKey(download) to serialize(download, nextOrder++)
            },
        )
    }

    fun remove(download: AnimeDownload) {
        backend.remove(setOf(getKey(download)))
    }

    fun removeAll(downloads: List<AnimeDownload>) {
        backend.remove(downloads.mapTo(mutableSetOf(), ::getKey))
    }

    @Synchronized
    fun clear() {
        backend.clear()
        nextOrder = 0
    }

    suspend fun restore(): List<AnimeDownload> {
        val objects = backend.values()
            .mapNotNull { (_, value) -> (value as? String)?.let(::deserialize) }
            .sortedBy(AnimeDownloadObject::order)

        val entriesByProfile = objects
            .mapNotNull(AnimeDownloadObject::profileId)
            .distinct()
            .associateWith { profileId ->
                runCatching { entryRepository.getAllEntriesByProfile(profileId) }
                    .getOrDefault(emptyList())
                    .associateBy(Entry::id)
            }

        val legacyEntries = mutableMapOf<Long, Entry?>()
        val downloads = objects.mapNotNull { obj ->
            val entry = if (obj.profileId != null) {
                entriesByProfile[obj.profileId]?.get(obj.animeId)
            } else {
                legacyEntries.getOrPut(obj.animeId) {
                    runCatching { entryRepository.getEntryById(obj.animeId) }.getOrNull()
                }
            }
            if (
                entry?.type != EntryType.ANIME ||
                entry.profileId != (obj.profileId ?: entry.profileId) ||
                (obj.sourceId != null && entry.source != obj.sourceId)
            ) {
                return@mapNotNull null
            }

            val episode = runCatching { entryChapterRepository.getChapterById(obj.episodeId) }.getOrNull()
                ?.takeIf { it.entryId == entry.id }
                ?: return@mapNotNull null

            AnimeDownload(
                anime = entry,
                episode = episode,
                preferences = DownloadPreferences(
                    entryId = entry.id,
                    dubKey = obj.dubKey,
                    serverKey = obj.serverKey,
                    streamKey = obj.streamKey,
                    subtitleKey = obj.subtitleKey,
                    audioKey = obj.audioKey,
                    qualityMode = obj.qualityMode.toQualityMode(),
                    updatedAt = obj.updatedAt,
                ),
            )
        }

        return downloads
    }

    private fun getKey(download: AnimeDownload): String =
        "${download.anime.profileId}:${download.anime.id}:${download.episode.id}"

    private fun serialize(download: AnimeDownload, order: Int): String {
        return json.encodeToString(
            AnimeDownloadObject(
                profileId = download.anime.profileId,
                animeId = download.anime.id,
                episodeId = download.episode.id,
                sourceId = download.anime.source,
                dubKey = download.preferences.dubKey,
                serverKey = download.preferences.serverKey,
                streamKey = download.preferences.streamKey,
                subtitleKey = download.preferences.subtitleKey,
                audioKey = download.preferences.audioKey,
                qualityMode = download.preferences.qualityMode.toDatabaseValue(),
                updatedAt = download.preferences.updatedAt,
                order = order,
            ),
        )
    }

    private fun deserialize(value: String): AnimeDownloadObject? {
        return runCatching { json.decodeFromString<AnimeDownloadObject>(value) }.getOrNull()
    }
}

internal interface AnimeDownloadStoreBackend {
    fun values(): Map<String, *>
    fun putAll(values: Map<String, String>)
    fun remove(keys: Set<String>)
    fun clear()
}

/** Keeps each download under its key in [preferences], which holds nothing else. */
private class PreferenceAnimeDownloadStoreBackend(
    private val preferences: PreferenceStore,
) : AnimeDownloadStoreBackend {
    override fun values(): Map<String, *> = preferences.getAll()

    override fun putAll(values: Map<String, String>) {
        values.forEach { (key, value) -> preferences.getString(key).set(value) }
    }

    override fun remove(keys: Set<String>) {
        keys.forEach { preferences.getString(it).delete() }
    }

    override fun clear() {
        remove(preferences.getAll().keys)
    }
}

@Serializable
private data class AnimeDownloadObject(
    // Null supports queues written by the pre-profile AnimeDownloadStore.
    val profileId: Long? = null,
    val animeId: Long,
    val episodeId: Long,
    val sourceId: Long? = null,
    val dubKey: String?,
    // Absent from queues written before servers.
    val serverKey: String? = null,
    val streamKey: String?,
    val subtitleKey: String?,
    // Absent from queues written before external audio.
    val audioKey: String? = null,
    val qualityMode: String,
    val updatedAt: Long,
    val order: Int,
)

private fun VideoDownloadQualityMode.toDatabaseValue(): String = when (this) {
    VideoDownloadQualityMode.BEST -> "best"
    VideoDownloadQualityMode.BALANCED -> "balanced"
    VideoDownloadQualityMode.DATA_SAVING -> "data_saving"
}

private fun String.toQualityMode(): VideoDownloadQualityMode = when (this) {
    "best" -> VideoDownloadQualityMode.BEST
    "data_saving" -> VideoDownloadQualityMode.DATA_SAVING
    else -> VideoDownloadQualityMode.BALANCED
}
