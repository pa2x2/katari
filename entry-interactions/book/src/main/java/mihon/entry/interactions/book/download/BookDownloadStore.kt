package mihon.entry.interactions.book.download

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mihon.entry.interactions.book.download.model.BookDownload
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
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
        backend = BookDownloadQueueFileBackend(
            snapshotFile = context.durableBookDownloadQueueDirectory().resolve(QUEUE_FILE_NAME),
            legacyPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE),
        ),
        json = json,
        entryRepository = entryRepository,
        entryChapterRepository = entryChapterRepository,
    )

    @Synchronized
    fun replace(downloads: List<BookDownload>) {
        backend.replace(
            downloads.mapIndexed { order, download ->
                "${download.entry.profileId}:${download.entry.id}:${download.chapter.id}" to json.encodeToString(
                    BookDownloadObject(
                        profileId = download.entry.profileId,
                        entryId = download.entry.id,
                        chapterId = download.chapter.id,
                        sourceId = download.entry.source,
                        order = order,
                    ),
                )
            }.toMap(),
        )
    }

    fun clear() = backend.clear()

    fun remove(downloads: Collection<BookDownload>) {
        backend.remove(downloads.mapTo(mutableSetOf()) { it.storageKey() })
    }

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

        val chaptersByEntry = objects
            .mapNotNull { stored -> entriesByProfile[stored.profileId]?.get(stored.entryId) }
            .distinctBy(Entry::id)
            .associate { entry ->
                entry.id to runCatching {
                    entryChapterRepository.getChaptersByEntryIdAwait(entry.id).associateBy(EntryChapter::id)
                }.getOrDefault(emptyMap())
            }

        return objects.mapNotNull { stored ->
            val entry = entriesByProfile[stored.profileId]?.get(stored.entryId)
                ?.takeIf {
                    it.type == EntryType.BOOK &&
                        it.profileId == stored.profileId &&
                        (stored.sourceId == null || it.source == stored.sourceId)
                }
                ?: return@mapNotNull null
            val chapter = chaptersByEntry[entry.id]?.get(stored.chapterId)
                ?: runCatching { entryChapterRepository.getChapterById(stored.chapterId) }.getOrNull()
                    ?.takeIf { it.entryId == entry.id }
                ?: return@mapNotNull null
            BookDownload(entry, chapter).apply { status = BookDownload.State.QUEUE }
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "active_book_downloads"
        const val QUEUE_FILE_NAME = "active_book_downloads_v2"
    }

    private fun BookDownload.storageKey(): String = "${entry.profileId}:${entry.id}:${chapter.id}"
}

internal interface BookDownloadStoreBackend {
    fun values(): Map<String, *>
    fun putAll(values: Map<String, String>)
    fun clear()

    fun remove(keys: Set<String>) = Unit

    fun replace(values: Map<String, String>) {
        clear()
        putAll(values)
    }
}

@Serializable
private data class BookDownloadObject(
    val profileId: Long,
    val entryId: Long,
    val chapterId: Long,
    val sourceId: Long? = null,
    val order: Int,
)

private fun Context.durableBookDownloadQueueDirectory() =
    runCatching { noBackupFilesDir }.getOrNull()
        ?.takeIf { directory -> runCatching { directory.path.isNotBlank() }.getOrDefault(false) }
        ?: filesDir
