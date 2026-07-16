package mihon.entry.interactions.book.download

import eu.kanade.tachiyomi.source.entry.EntryType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mihon.entry.interactions.book.download.model.BookDownload
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import kotlin.test.assertEquals

class BookDownloadStoreTest {
    @Test
    fun `restore retains queue order and resets items to queued`() = runTest {
        val entry = Entry.create().copy(
            id = 1L,
            profileId = 7L,
            source = 42L,
            url = "/book",
            title = "Book",
            type = EntryType.BOOK,
        )
        val first = chapter(11L, entry.id)
        val second = chapter(12L, entry.id)
        val backend = MemoryBookDownloadStoreBackend()
        val entryRepository = mockk<EntryRepository> {
            coEvery { getAllEntriesByProfile(entry.profileId) } returns listOf(entry)
        }
        val chapterRepository = mockk<EntryChapterRepository> {
            coEvery { getChapterById(first.id) } returns first
            coEvery { getChapterById(second.id) } returns second
        }
        val store = BookDownloadStore(backend, Json, entryRepository, chapterRepository)
        store.replace(listOf(BookDownload(entry, second), BookDownload(entry, first)))

        val restored = store.restore()

        assertEquals(listOf(second.id, first.id), restored.map { it.chapter.id })
        assertEquals(listOf(BookDownload.State.QUEUE, BookDownload.State.QUEUE), restored.map { it.status })
    }

    private fun chapter(id: Long, entryId: Long) = EntryChapter.create().copy(
        id = id,
        entryId = entryId,
        url = "/chapter/$id",
        name = "Chapter $id",
    )
}

private class MemoryBookDownloadStoreBackend : BookDownloadStoreBackend {
    private val values = linkedMapOf<String, String>()
    override fun values(): Map<String, *> = values.toMap()
    override fun putAll(values: Map<String, String>) {
        this.values.putAll(values)
    }

    override fun clear() {
        values.clear()
    }
}
