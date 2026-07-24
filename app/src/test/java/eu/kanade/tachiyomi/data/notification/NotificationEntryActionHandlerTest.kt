package eu.kanade.tachiyomi.data.notification

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.EntryConsumptionFeature
import mihon.entry.interactions.EntryConsumptionResult
import mihon.entry.interactions.EntryDownloadActionFeature
import mihon.entry.interactions.EntryOpenFeature
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
    private val entryConsumptionFeature = mockk<EntryConsumptionFeature>()
    private val entryDownloadActionFeature = mockk<EntryDownloadActionFeature>(relaxed = true)
    private val entryOpenFeature = mockk<EntryOpenFeature>(relaxed = true) {
        every { open(any(), any(), any(), any()) } returns true
    }
    private val handler = NotificationEntryActionHandler(
        entryRepository = entryRepository,
        entryChapterRepository = entryChapterRepository,
        entryConsumptionFeature = entryConsumptionFeature,
        entryDownloadActionFeature = entryDownloadActionFeature,
        entryOpenFeature = entryOpenFeature,
    )

    @Test
    fun `mark consumed loads entry and children then marks them consumed`() = runTest {
        val entry = entry(EntryType.MANGA, id = 1L)
        val chapters = listOf(chapter(id = 10L, entryId = 1L), chapter(id = 11L, entryId = 1L))
        coEvery { entryRepository.getEntryById(1L, 7L) } returns entry
        coEvery { entryChapterRepository.getChapterById(10L) } returns chapters[0]
        coEvery { entryChapterRepository.getChapterById(11L) } returns chapters[1]
        coEvery {
            entryConsumptionFeature.setConsumed(entry, chapters, consumed = true)
        } returns EntryConsumptionResult.Changed(chapters)

        handler.markConsumed(profileId = 7L, entryId = 1L, childIds = longArrayOf(10L, 11L))

        coVerify { entryConsumptionFeature.setConsumed(entry, chapters, consumed = true) }
    }

    @Test
    fun `download children loads entry and children then downloads them`() = runTest {
        val entry = entry(EntryType.MANGA, id = 2L)
        val chapters = listOf(chapter(id = 20L, entryId = 2L), chapter(id = 21L, entryId = 2L))
        coEvery { entryRepository.getEntryById(2L, 7L) } returns entry
        coEvery { entryChapterRepository.getChapterById(20L) } returns chapters[0]
        coEvery { entryChapterRepository.getChapterById(21L) } returns chapters[1]

        handler.downloadChildren(profileId = 7L, entryId = 2L, childIds = longArrayOf(20L, 21L))

        coVerify { entryDownloadActionFeature.download(entry, chapters, false) }
    }

    @Test
    fun `mark consumed by urls normalizes to child ids and marks matching children consumed`() = runTest {
        val entry = entry(EntryType.MANGA, id = 6L)
        val chapters = listOf(
            chapter(id = 60L, entryId = 6L, url = "chapter-1"),
            chapter(id = 61L, entryId = 6L, url = "chapter-2"),
            chapter(id = 62L, entryId = 6L, url = "chapter-3"),
        )
        coEvery { entryRepository.getEntryById(6L, 7L) } returns entry
        coEvery { entryChapterRepository.getChaptersByEntryIdAwait(6L) } returns chapters
        coEvery { entryChapterRepository.getChapterById(60L) } returns chapters[0]
        coEvery { entryChapterRepository.getChapterById(62L) } returns chapters[2]
        coEvery {
            entryConsumptionFeature.setConsumed(entry, listOf(chapters[0], chapters[2]), consumed = true)
        } returns EntryConsumptionResult.Changed(listOf(chapters[0], chapters[2]))

        handler.markConsumed(profileId = 7L, entryId = 6L, childUrls = arrayOf("chapter-1", "chapter-3"))

        coVerify {
            entryConsumptionFeature.setConsumed(
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
        coEvery { entryRepository.getEntryById(7L, 7L) } returns entry
        coEvery { entryChapterRepository.getChaptersByEntryIdAwait(7L) } returns chapters
        coEvery { entryChapterRepository.getChapterById(71L) } returns chapters[1]
        coEvery { entryChapterRepository.getChapterById(72L) } returns chapters[2]

        handler.downloadChildren(profileId = 7L, entryId = 7L, childUrls = arrayOf("chapter-2", "chapter-3"))

        coVerify { entryDownloadActionFeature.download(entry, listOf(chapters[1], chapters[2]), false) }
    }

    @Test
    fun `open child opens manga with owner entry id`() = runTest {
        val visibleEntry = entry(EntryType.MANGA, id = 3L)
        val chapter = chapter(id = 30L, entryId = 3L)
        coEvery { entryRepository.getEntryById(3L, 7L) } returns visibleEntry
        coEvery { entryChapterRepository.getChapterById(30L) } returns chapter

        val opened = handler.openChild(context, 7L, visibleEntryId = 3L, ownerEntryId = 3L, childId = 30L)

        opened shouldBe true
        val options = slot<EntryOpenOptions>()
        verify { entryOpenFeature.open(context, visibleEntry, chapter, capture(options)) }
        options.captured.ownerEntryId shouldBe 3L
        options.captured.newTask shouldBe true
        options.captured.clearTop shouldBe true
    }

    @Test
    fun `open child opens anime with owner entry id`() = runTest {
        val visibleEntry = entry(EntryType.ANIME, id = 4L)
        val ownerEntry = entry(EntryType.ANIME, id = 5L)
        val episode = chapter(id = 40L, entryId = 5L)
        coEvery { entryRepository.getEntryById(4L, 7L) } returns visibleEntry
        coEvery { entryRepository.getEntryById(5L, 7L) } returns ownerEntry
        coEvery { entryChapterRepository.getChapterById(40L) } returns episode

        val opened = handler.openChild(context, 7L, visibleEntryId = 4L, ownerEntryId = 5L, childId = 40L)

        opened shouldBe true
        val options = slot<EntryOpenOptions>()
        verify { entryOpenFeature.open(context, visibleEntry, episode, capture(options)) }
        options.captured.ownerEntryId shouldBe 5L
        options.captured.newTask shouldBe true
        options.captured.clearTop shouldBe true
    }

    @Test
    fun `open child returns false when entry is missing`() = runTest {
        coEvery { entryRepository.getEntryById(8L, 7L) } returns null

        val opened = handler.openChild(context, 7L, visibleEntryId = 8L, ownerEntryId = 8L, childId = 80L)

        opened shouldBe false
    }

    @Test
    fun `open child returns false when Open is not applicable`() = runTest {
        val visibleEntry = entry(EntryType.BOOK, id = 9L)
        val chapter = chapter(id = 90L, entryId = 9L)
        coEvery { entryRepository.getEntryById(9L, 7L) } returns visibleEntry
        coEvery { entryChapterRepository.getChapterById(90L) } returns chapter
        every { entryOpenFeature.open(any(), any(), any(), any()) } returns false

        val opened = handler.openChild(context, 7L, visibleEntryId = 9L, ownerEntryId = 9L, childId = 90L)

        opened shouldBe false
    }

    private fun entry(type: EntryType, id: Long): Entry {
        return Entry.create().copy(id = id, profileId = 7L, type = type)
    }

    private fun chapter(id: Long, entryId: Long, url: String = ""): EntryChapter {
        return EntryChapter.create().copy(id = id, entryId = entryId, url = url)
    }
}
