package eu.kanade.tachiyomi.data.notification

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.EntryConsumptionInteraction
import mihon.entry.interactions.EntryDownloadInteraction
import mihon.entry.interactions.EntryOpenInteraction
import mihon.entry.interactions.EntryOpenOptions
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository

class NotificationEntryActionHandlerTest {

    private val context = mockk<Context>(relaxed = true)
    private val entryRepository = mockk<EntryRepository>()
    private val entryChapterRepository = mockk<EntryChapterRepository>()
    private val entryConsumptionInteraction = mockk<EntryConsumptionInteraction>(relaxed = true)
    private val entryDownloadInteraction = mockk<EntryDownloadInteraction>(relaxed = true)
    private val entryOpenInteraction = mockk<EntryOpenInteraction>(relaxed = true)
    private val handler = NotificationEntryActionHandler(
        entryRepository = entryRepository,
        entryChapterRepository = entryChapterRepository,
        entryConsumptionInteraction = entryConsumptionInteraction,
        entryDownloadInteraction = entryDownloadInteraction,
        entryOpenInteraction = entryOpenInteraction,
    )

    @Test
    fun `mark consumed loads entry and children then marks them consumed`() = runTest {
        val entry = entry(EntryType.MANGA, id = 1L)
        val chapters = listOf(chapter(id = 10L, entryId = 1L), chapter(id = 11L, entryId = 1L))
        coEvery { entryRepository.getEntryById(1L) } returns entry
        coEvery { entryChapterRepository.getChapterById(10L) } returns chapters[0]
        coEvery { entryChapterRepository.getChapterById(11L) } returns chapters[1]

        handler.markConsumed(entryId = 1L, childIds = longArrayOf(10L, 11L))

        coVerify { entryConsumptionInteraction.setConsumed(entry, chapters, consumed = true) }
    }

    @Test
    fun `download children loads entry and children then downloads them`() = runTest {
        val entry = entry(EntryType.MANGA, id = 2L)
        val chapters = listOf(chapter(id = 20L, entryId = 2L), chapter(id = 21L, entryId = 2L))
        coEvery { entryRepository.getEntryById(2L) } returns entry
        coEvery { entryChapterRepository.getChapterById(20L) } returns chapters[0]
        coEvery { entryChapterRepository.getChapterById(21L) } returns chapters[1]

        handler.downloadChildren(entryId = 2L, childIds = longArrayOf(20L, 21L))

        coVerify { entryDownloadInteraction.download(entry, chapters) }
    }

    @Test
    fun `mark consumed by urls normalizes to child ids and marks matching children consumed`() = runTest {
        val entry = entry(EntryType.MANGA, id = 6L)
        val chapters = listOf(
            chapter(id = 60L, entryId = 6L, url = "chapter-1"),
            chapter(id = 61L, entryId = 6L, url = "chapter-2"),
            chapter(id = 62L, entryId = 6L, url = "chapter-3"),
        )
        coEvery { entryRepository.getEntryById(6L) } returns entry
        coEvery { entryChapterRepository.getChaptersByEntryIdAwait(6L) } returns chapters
        coEvery { entryChapterRepository.getChapterById(60L) } returns chapters[0]
        coEvery { entryChapterRepository.getChapterById(62L) } returns chapters[2]

        handler.markConsumed(entryId = 6L, childUrls = arrayOf("chapter-1", "chapter-3"))

        coVerify {
            entryConsumptionInteraction.setConsumed(
                entry,
                listOf(chapters[0], chapters[2]),
                consumed = true,
            )
        }
    }

    @Test
    fun `download children by urls normalizes to child ids and downloads matching children`() = runTest {
        val entry = entry(EntryType.MANGA, id = 7L)
        val chapters = listOf(
            chapter(id = 70L, entryId = 7L, url = "chapter-1"),
            chapter(id = 71L, entryId = 7L, url = "chapter-2"),
            chapter(id = 72L, entryId = 7L, url = "chapter-3"),
        )
        coEvery { entryRepository.getEntryById(7L) } returns entry
        coEvery { entryChapterRepository.getChaptersByEntryIdAwait(7L) } returns chapters
        coEvery { entryChapterRepository.getChapterById(71L) } returns chapters[1]
        coEvery { entryChapterRepository.getChapterById(72L) } returns chapters[2]

        handler.downloadChildren(entryId = 7L, childUrls = arrayOf("chapter-2", "chapter-3"))

        coVerify { entryDownloadInteraction.download(entry, listOf(chapters[1], chapters[2])) }
    }

    @Test
    fun `open child opens manga with owner entry id`() = runTest {
        val visibleEntry = entry(EntryType.MANGA, id = 3L)
        val chapter = chapter(id = 30L, entryId = 3L)
        coEvery { entryRepository.getEntryById(3L) } returns visibleEntry
        coEvery { entryChapterRepository.getChapterById(30L) } returns chapter

        val opened = handler.openChild(context, visibleEntryId = 3L, ownerEntryId = 3L, childId = 30L)

        opened shouldBe true
        val options = slot<EntryOpenOptions>()
        verify { entryOpenInteraction.open(context, visibleEntry, chapter, capture(options)) }
        options.captured.ownerEntryId shouldBe 3L
        options.captured.newTask shouldBe true
        options.captured.clearTop shouldBe true
    }

    @Test
    fun `open child opens anime with owner entry id`() = runTest {
        val visibleEntry = entry(EntryType.ANIME, id = 4L)
        val episode = chapter(id = 40L, entryId = 5L)
        coEvery { entryRepository.getEntryById(4L) } returns visibleEntry
        coEvery { entryChapterRepository.getChapterById(40L) } returns episode

        val opened = handler.openChild(context, visibleEntryId = 4L, ownerEntryId = 5L, childId = 40L)

        opened shouldBe true
        val options = slot<EntryOpenOptions>()
        verify { entryOpenInteraction.open(context, visibleEntry, episode, capture(options)) }
        options.captured.ownerEntryId shouldBe 5L
        options.captured.newTask shouldBe true
        options.captured.clearTop shouldBe true
    }

    @Test
    fun `open child returns false when entry is missing`() = runTest {
        coEvery { entryRepository.getEntryById(8L) } returns null

        val opened = handler.openChild(context, visibleEntryId = 8L, ownerEntryId = 8L, childId = 80L)

        opened shouldBe false
    }

    private fun entry(type: EntryType, id: Long): Entry {
        return Entry.create().copy(id = id, type = type)
    }

    private fun chapter(id: Long, entryId: Long, url: String = ""): EntryChapter {
        return EntryChapter.create().copy(id = id, entryId = entryId, url = url)
    }
}
