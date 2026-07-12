package tachiyomi.domain.entry.interactor

import eu.kanade.tachiyomi.source.entry.ChapterNumberRecognitionSource
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryPageResult
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.IncrementalChapterSource
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.chapter.model.NoChaptersException
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.service.EntryMetadataUpdateHooks
import tachiyomi.domain.entry.service.FetchInterval
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.service.SourceManager

class SyncEntryWithSourceTest {

    @Test
    fun `empty non-capable source list preserves stored chapters`() = runTest {
        val entry = entry()
        val repository = chapterRepository(listOf(chapter(id = 1L)))
        val sync = sync(TestSource(chapters = emptyList()), repository = repository)

        val error = runCatching { sync(entry, fetchDetails = false) }.exceptionOrNull()

        (error is NoChaptersException) shouldBe true
        coVerify(exactly = 0) { repository.removeChaptersWithIds(any()) }
    }

    @Test
    fun `incremental legacy source receives stored chapters and recognized numbers are persisted`() = runTest {
        val existing = chapter(id = 1L, url = "/old")
        val sourceChapter = sourceChapter(url = "/new", name = "Chapter 12", chapterNumber = -1.0)
        val source = IncrementalRecognizingSource(listOf(sourceChapter))
        val repository = chapterRepository(listOf(existing))
        val inserted = slot<List<EntryChapter>>()
        coEvery { repository.insertOrUpdate(capture(inserted)) } answers {
            inserted.captured.mapIndexed { index, item -> item.copy(id = 10L + index) }
        }
        coEvery { repository.getChaptersByEntryIdAwait(1L, true) } answers {
            inserted.captured.mapIndexed { index, item -> item.copy(id = 10L + index) }
        }

        val result = sync(source, repository = repository)(entry(), fetchDetails = false)

        source.existingChapters.single().url shouldBe "/old"
        inserted.captured.single().chapterNumber shouldBe 12.0
        result.insertedChapters.single().chapterNumber shouldBe 12.0
    }

    @Test
    fun `chapter updates copy source memo`() = runTest {
        val oldMemo = buildJsonObject { put("token", JsonPrimitive("old")) }
        val newMemo = buildJsonObject { put("token", JsonPrimitive("new")) }
        val existing = chapter(id = 1L, memo = oldMemo)
        val source = TestSource(chapters = listOf(sourceChapter(memo = newMemo)))
        val repository = chapterRepository(listOf(existing))
        val updates = slot<List<EntryChapter>>()
        coEvery { repository.updateAll(capture(updates)) } returns true

        sync(source, repository = repository)(entry(), fetchDetails = false)

        updates.captured.single().memo shouldBe newMemo
    }

    @Test
    fun `manual metadata refresh invalidates same url cover but respects title preference`() = runTest {
        val entry = entry().copy(
            favorite = true,
            title = "Stored title",
            thumbnailUrl = "/cover",
            coverLastModified = 100L,
        )
        val details = sourceEntry(title = "Remote title", thumbnailUrl = "/cover")
        val entryRepository = mockEntryRepository()
        val updated = slot<Entry>()
        coEvery { entryRepository.update(capture(updated)) } returns true
        val hooks = mockk<EntryMetadataUpdateHooks>(relaxed = true)

        sync(
            source = TestSource(details = details),
            entryRepository = entryRepository,
            metadataUpdateHooks = hooks,
            now = { 200L },
        )(entry, fetchChapters = false, manualFetch = true)

        updated.captured.title shouldBe "Stored title"
        updated.captured.coverLastModified shouldBe 200L
        coVerify(exactly = 0) { hooks.onTitleChanged(any(), any()) }
    }

    @Test
    fun `allowed source title change invokes metadata hook`() = runTest {
        val preferences = LibraryPreferences(InMemoryPreferenceStore()).also {
            it.updateMangaTitles.set(true)
        }
        val hooks = mockk<EntryMetadataUpdateHooks>(relaxed = true)
        val stored = entry().copy(favorite = true, title = "Stored title")
        val source = TestSource(details = sourceEntry(title = "Remote title"))

        sync(source, libraryPreferences = preferences, metadataUpdateHooks = hooks)(
            stored,
            fetchChapters = false,
        )

        coVerify(exactly = 1) { hooks.onTitleChanged(stored, "Remote title") }
    }

    @Test
    fun `manual no-change refresh advances fetch interval`() = runTest {
        val existing = chapter(id = 1L)
        val repository = chapterRepository(listOf(existing))
        val fetchInterval = mockk<FetchInterval>()
        coEvery { fetchInterval.update(any(), any(), any()) } answers {
            firstArg<Entry>().copy(nextUpdate = 500L, fetchInterval = 7)
        }
        val entryRepository = mockEntryRepository()
        val updated = slot<Entry>()
        coEvery { entryRepository.update(capture(updated)) } returns true

        sync(
            source = TestSource(chapters = listOf(sourceChapter())),
            repository = repository,
            entryRepository = entryRepository,
            fetchInterval = fetchInterval,
        )(entry().copy(fetchInterval = 7), fetchDetails = false, manualFetch = true)

        updated.captured.nextUpdate shouldBe 500L
        coVerify(exactly = 1) { fetchInterval.update(any(), any(), any()) }
    }

    @Test
    fun `inserted chapters excluded by scanlator filter are not reported`() = runTest {
        val repository = chapterRepository(emptyList())
        coEvery { repository.insertOrUpdate(any()) } answers {
            firstArg<List<EntryChapter>>().map { it.copy(id = 10L) }
        }
        coEvery { repository.getChaptersByEntryIdAwait(1L, true) } returns emptyList()

        val result = sync(
            source = TestSource(chapters = listOf(sourceChapter(scanlator = "hidden"))),
            repository = repository,
        )(entry(), fetchDetails = false)

        result.insertedChapters.shouldBeEmpty()
        result.insertedChaptersTotal shouldBe 1
        result.hasChanges shouldBe true
    }

    private fun sync(
        source: UnifiedSource,
        repository: EntryChapterRepository = chapterRepository(emptyList()),
        entryRepository: EntryRepository = mockEntryRepository(),
        libraryPreferences: LibraryPreferences = LibraryPreferences(InMemoryPreferenceStore()),
        fetchInterval: FetchInterval = mockFetchInterval(),
        metadataUpdateHooks: EntryMetadataUpdateHooks = mockk(relaxed = true),
        now: () -> Long = { 1000L },
    ): SyncEntryWithSource {
        val sourceManager = mockk<SourceManager> {
            every { get(1L) } returns source
        }
        return SyncEntryWithSource(
            entryRepository = entryRepository,
            entryChapterRepository = repository,
            sourceManager = sourceManager,
            libraryPreferences = libraryPreferences,
            fetchInterval = fetchInterval,
            metadataUpdateHooks = metadataUpdateHooks,
            now = now,
        )
    }

    private fun mockEntryRepository(): EntryRepository = mockk(relaxed = true) {
        coEvery { update(any()) } returns true
    }

    private fun mockFetchInterval(): FetchInterval = mockk {
        coEvery { update(any(), any(), any()) } answers { firstArg() }
    }

    private fun chapterRepository(chapters: List<EntryChapter>): EntryChapterRepository = mockk(relaxed = true) {
        coEvery { getChaptersByEntryIdAwait(1L, false) } returns chapters
        coEvery { getChaptersByEntryIdAwait(1L, true) } returns chapters
        coEvery { updateAll(any()) } returns true
        coEvery { insertOrUpdate(any()) } returns emptyList()
    }

    private fun entry(): Entry = Entry.create().copy(
        id = 1L,
        source = 1L,
        title = "Entry",
        type = EntryType.MANGA,
    )

    private fun chapter(
        id: Long,
        url: String = "/chapter",
        memo: kotlinx.serialization.json.JsonObject = buildJsonObject {},
    ): EntryChapter = EntryChapter.create().copy(
        id = id,
        entryId = 1L,
        url = url,
        name = "Chapter 1",
        chapterNumber = 1.0,
        memo = memo,
    )

    private fun sourceEntry(
        title: String = "Entry",
        thumbnailUrl: String? = null,
    ): SEntry = SEntry.create().apply {
        url = "/entry"
        this.title = title
        this.thumbnailUrl = thumbnailUrl
        type = EntryType.MANGA
    }

    private fun sourceChapter(
        url: String = "/chapter",
        name: String = "Chapter 1",
        chapterNumber: Double = 1.0,
        scanlator: String? = null,
        memo: kotlinx.serialization.json.JsonObject = buildJsonObject {},
    ): SEntryChapter = SEntryChapter.create().apply {
        this.url = url
        this.name = name
        this.chapterNumber = chapterNumber
        this.scanlator = scanlator
        this.memo = memo
    }
}

private open class TestSource(
    private val details: SEntry? = null,
    private val chapters: List<SEntryChapter> = emptyList(),
) : UnifiedSource {
    override val id: Long = 1L
    override val name: String = "Source"

    override suspend fun getPopularContent(page: Int): EntryPageResult<SEntry> = error("Not used")
    override suspend fun getLatestUpdates(page: Int): EntryPageResult<SEntry> = error("Not used")
    override suspend fun getSearchContent(
        page: Int,
        query: String,
        filters: EntryFilterList,
    ): EntryPageResult<SEntry> = error("Not used")

    override suspend fun getContentDetails(entry: SEntry): SEntry = details ?: entry
    override suspend fun getChapterList(entry: SEntry): List<SEntryChapter> = chapters
    override suspend fun getMedia(chapter: SEntryChapter, selection: PlaybackSelection): EntryMedia = error("Not used")
}

private class IncrementalRecognizingSource(
    private val chapters: List<SEntryChapter>,
) : TestSource(), IncrementalChapterSource, ChapterNumberRecognitionSource {
    var existingChapters: List<SEntryChapter> = emptyList()

    override suspend fun getChapterList(
        entry: SEntry,
        existingChapters: List<SEntryChapter>,
    ): List<SEntryChapter> {
        this.existingChapters = existingChapters
        return chapters
    }
}
