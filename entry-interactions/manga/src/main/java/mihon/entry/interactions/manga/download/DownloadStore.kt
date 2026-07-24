package mihon.entry.interactions.manga.download

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mihon.entry.interactions.manga.download.model.MangaDownload
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * This class is used to persist active downloads across application restarts.
 */
internal class DownloadStore(
    private val backend: MangaDownloadStoreBackend,
    private val sourceManager: SourceManager = Injekt.get(),
    private val json: Json = Injekt.get(),
    private val entryRepository: EntryRepository = Injekt.get(),
    private val entryChapterRepository: EntryChapterRepository = Injekt.get(),
) {
    constructor(
        context: Context,
        sourceManager: SourceManager = Injekt.get(),
        json: Json = Injekt.get(),
        entryRepository: EntryRepository = Injekt.get(),
        entryChapterRepository: EntryChapterRepository = Injekt.get(),
    ) : this(
        backend = SharedPreferencesMangaDownloadStoreBackend(
            context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE),
        ),
        sourceManager = sourceManager,
        json = json,
        entryRepository = entryRepository,
        entryChapterRepository = entryChapterRepository,
    )

    /**
     * Counter used to keep the queue order.
     */
    private var counter = 0

    /**
     * Adds a list of downloads to the store.
     *
     * @param downloads the list of downloads to add.
     */
    fun addAll(downloads: List<MangaDownload>) {
        backend.putAll(downloads.associate { getKey(it) to serialize(it) })
    }

    /**
     * Removes a download from the store.
     *
     * @param download the download to remove.
     */
    fun remove(download: MangaDownload) {
        backend.remove(setOf(getKey(download)))
    }

    /**
     * Removes a list of downloads from the store.
     *
     * @param downloads the download to remove.
     */
    fun removeAll(downloads: List<MangaDownload>) {
        backend.remove(downloads.mapTo(mutableSetOf(), ::getKey))
    }

    /**
     * Removes all the downloads from the store.
     */
    fun clear() {
        backend.clear()
    }

    /**
     * Returns the preference's key for the given download.
     *
     * @param download the download.
     */
    private fun getKey(download: MangaDownload): String {
        return "${download.entry.profileId}:${download.entry.id}:${download.chapter.id}"
    }

    /**
     * Returns the list of downloads to restore. It should be called in a background thread.
     */
    fun restore(): List<MangaDownload> {
        val objs = backend.values()
            .mapNotNull { (_, value) -> value as? String }
            .mapNotNull { deserialize(it) }
            .sortedBy { it.order }

        val entriesByProfile = objs
            .mapNotNull(DownloadObject::profileId)
            .distinct()
            .associateWith { profileId ->
                runBlocking { entryRepository.getAllEntriesByProfile(profileId) }.associateBy(Entry::id)
            }
        val legacyEntries = mutableMapOf<Long, Entry?>()
        val downloads = objs.mapNotNull { stored ->
            val entry = if (stored.profileId != null) {
                entriesByProfile[stored.profileId]?.get(stored.mangaId)
            } else {
                legacyEntries.getOrPut(stored.mangaId) {
                    runBlocking { entryRepository.getEntryById(stored.mangaId) }
                }
            }?.takeIf {
                it.type == EntryType.MANGA &&
                    (stored.profileId == null || it.profileId == stored.profileId) &&
                    (stored.sourceId == null || it.source == stored.sourceId)
            } ?: return@mapNotNull null
            val source = sourceManager.get(entry.source) ?: return@mapNotNull null
            val chapter = runBlocking { entryChapterRepository.getChapterById(stored.chapterId) }
                ?.takeIf { it.entryId == entry.id }
                ?: return@mapNotNull null
            MangaDownload(source, entry, chapter)
        }

        // Clear the store, downloads will be added again immediately.
        clear()
        return downloads
    }

    /**
     * Converts a download to a string.
     *
     * @param download the download to serialize.
     */
    private fun serialize(download: MangaDownload): String {
        val obj = DownloadObject(
            mangaId = download.entry.id,
            chapterId = download.chapter.id,
            order = counter++,
            profileId = download.entry.profileId,
            sourceId = download.entry.source,
        )
        return json.encodeToString(obj)
    }

    /**
     * Restore a download from a string.
     *
     * @param string the download as string.
     */
    private fun deserialize(string: String): DownloadObject? {
        return try {
            json.decodeFromString<DownloadObject>(string)
        } catch (e: Exception) {
            null
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "active_downloads"
    }
}

internal interface MangaDownloadStoreBackend {
    fun values(): Map<String, *>
    fun putAll(values: Map<String, String>)
    fun remove(keys: Set<String>)
    fun clear()
}

private class SharedPreferencesMangaDownloadStoreBackend(
    private val preferences: SharedPreferences,
) : MangaDownloadStoreBackend {
    override fun values(): Map<String, *> = preferences.all

    override fun putAll(values: Map<String, String>) {
        preferences.edit { values.forEach(::putString) }
    }

    override fun remove(keys: Set<String>) {
        preferences.edit { keys.forEach(::remove) }
    }

    override fun clear() {
        preferences.edit { clear() }
    }
}

/**
 * Class used for download serialization
 *
 * @param mangaId the id of the manga.
 * @param chapterId the id of the chapter.
 * @param order the order of the download in the queue.
 */
@Serializable
private data class DownloadObject(
    val mangaId: Long,
    val chapterId: Long,
    val order: Int,
    val profileId: Long? = null,
    val sourceId: Long? = null,
)
