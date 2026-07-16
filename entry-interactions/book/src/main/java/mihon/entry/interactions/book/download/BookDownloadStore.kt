package mihon.entry.interactions.book.download

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mihon.entry.interactions.book.download.model.BookDownload
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Persists the ordered BOOK queue across process and worker restarts. */
internal class BookDownloadStore(
    private val backend: BookDownloadStoreBackend,
    private val json: Json,
    private val entryRepository: EntryRepository,
    private val entryChapterRepository: EntryChapterRepository,
) {
    constructor(
        context: Context,
        json: Json = Injekt.get(),
        entryRepository: EntryRepository = Injekt.get(),
        entryChapterRepository: EntryChapterRepository = Injekt.get(),
    ) : this(
        backend = SharedPreferencesBookDownloadStoreBackend(
            context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE),
        ),
        json = json,
        entryRepository = entryRepository,
        entryChapterRepository = entryChapterRepository,
    )

    @Synchronized
    fun replace(downloads: List<BookDownload>) {
        backend.clear()
        backend.putAll(
            downloads.mapIndexed { order, download ->
                download.chapter.id.toString() to json.encodeToString(
                    BookDownloadObject(
                        profileId = download.entry.profileId,
                        entryId = download.entry.id,
                        chapterId = download.chapter.id,
                        order = order,
                    ),
                )
            }.toMap(),
        )
    }

    fun clear() = backend.clear()

    suspend fun restore(): List<BookDownload> {
        val objects = backend.values()
            .mapNotNull { (_, value) ->
                (value as? String)?.let { runCatching { json.decodeFromString<BookDownloadObject>(it) }.getOrNull() }
            }
            .sortedBy(BookDownloadObject::order)
        val entriesByProfile = objects
            .map(BookDownloadObject::profileId)
            .distinct()
            .associateWith { profileId ->
                runCatching { entryRepository.getAllEntriesByProfile(profileId) }
                    .getOrDefault(emptyList())
                    .associateBy(Entry::id)
            }

        return objects.mapNotNull { stored ->
            val entry = entriesByProfile[stored.profileId]?.get(stored.entryId)
                ?.takeIf { it.type == EntryType.BOOK && it.profileId == stored.profileId }
                ?: return@mapNotNull null
            val chapter = runCatching { entryChapterRepository.getChapterById(stored.chapterId) }.getOrNull()
                ?.takeIf { it.entryId == entry.id }
                ?: return@mapNotNull null
            BookDownload(entry, chapter).apply { status = BookDownload.State.QUEUE }
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "active_book_downloads"
    }
}

internal interface BookDownloadStoreBackend {
    fun values(): Map<String, *>
    fun putAll(values: Map<String, String>)
    fun clear()
}

private class SharedPreferencesBookDownloadStoreBackend(
    private val preferences: SharedPreferences,
) : BookDownloadStoreBackend {
    override fun values(): Map<String, *> = preferences.all

    override fun putAll(values: Map<String, String>) {
        preferences.edit { values.forEach(::putString) }
    }

    override fun clear() {
        preferences.edit { clear() }
    }
}

@Serializable
private data class BookDownloadObject(
    val profileId: Long,
    val entryId: Long,
    val chapterId: Long,
    val order: Int,
)
