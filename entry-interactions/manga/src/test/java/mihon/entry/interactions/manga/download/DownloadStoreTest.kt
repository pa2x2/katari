package mihon.entry.interactions.manga.download

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import mihon.entry.interactions.manga.download.model.MangaDownload
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.source.service.SourceManager

class DownloadStoreTest {
    private val backend = MemoryMangaDownloadStoreBackend()
    private val source = mockk<UnifiedSource> {
        every { id } returns 42L
    }
    private val sourceManager = mockk<SourceManager> {
        every { get(42L) } returns source
    }
    private val entryRepository = mockk<EntryRepository>()
    private val chapterRepository = mockk<EntryChapterRepository>()

    @Test
    fun `restores profile scoped owner identity in queue order`() {
        val firstEntry = manga(id = 1L, profileId = 10L)
        val secondEntry = manga(id = 2L, profileId = 20L)
        val first = download(firstEntry, chapterId = 11L)
        val second = download(secondEntry, chapterId = 22L)
        store().addAll(listOf(second, first))
        backend.data.keys.toList() shouldContainExactly listOf("20:2:22", "10:1:11")
        coEvery { entryRepository.getAllEntriesByProfile(10L) } returns listOf(firstEntry)
        coEvery { entryRepository.getAllEntriesByProfile(20L) } returns listOf(secondEntry)
        coEvery { chapterRepository.getChapterById(11L) } returns first.chapter
        coEvery { chapterRepository.getChapterById(22L) } returns second.chapter

        val restored = store().restore()

        restored.map { it.chapter.id } shouldContainExactly listOf(22L, 11L)
        restored.map { it.entry.profileId } shouldContainExactly listOf(20L, 10L)
        backend.data shouldBe emptyMap()
        coVerify(exactly = 0) { entryRepository.getEntryById(any()) }
    }

    @Test
    fun `restores legacy identity and rejects a changed source`() {
        val entry = manga(id = 5L, profileId = 10L)
        val chapter = EntryChapter.create().copy(id = 6L, entryId = entry.id)
        backend.data["legacy"] = """{"mangaId":5,"chapterId":6,"order":0}"""
        backend.data["wrong-source"] =
            """{"mangaId":5,"chapterId":6,"order":1,"profileId":10,"sourceId":99}"""
        coEvery { entryRepository.getEntryById(entry.id) } returns entry
        coEvery { entryRepository.getAllEntriesByProfile(entry.profileId) } returns listOf(entry)
        coEvery { chapterRepository.getChapterById(chapter.id) } returns chapter

        val restored = store().restore()

        restored.single().entry shouldBe entry
        restored.single().chapter shouldBe chapter
    }

    private fun store() = DownloadStore(
        backend = backend,
        sourceManager = sourceManager,
        json = Json,
        entryRepository = entryRepository,
        entryChapterRepository = chapterRepository,
    )

    private fun manga(id: Long, profileId: Long): Entry = Entry.create().copy(
        id = id,
        profileId = profileId,
        source = source.id,
        type = EntryType.MANGA,
    )

    private fun download(entry: Entry, chapterId: Long): MangaDownload {
        return MangaDownload(
            source = source,
            entry = entry,
            chapter = EntryChapter.create().copy(id = chapterId, entryId = entry.id),
        )
    }
}

private class MemoryMangaDownloadStoreBackend : MangaDownloadStoreBackend {
    val data = linkedMapOf<String, String>()

    override fun values(): Map<String, *> = data.toMap()

    override fun putAll(values: Map<String, String>) {
        data.putAll(values)
    }

    override fun remove(keys: Set<String>) {
        data.keys.removeAll(keys)
    }

    override fun clear() {
        data.clear()
    }
}
