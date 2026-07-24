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
import io.kotest.matchers.types.shouldBeInstanceOf
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
import tachiyomi.domain.entry.model.EntryProgressLocator
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.service.EntryMetadataChangeNotifier
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
    fun `chapter url changes rekey generic progress`() = runTest {
        val existing = chapter(id = 1L, url = "/old")
        val source = TestSource(chapters = listOf(sourceChapter(url = "/new")))
        val repository = chapterRepository(listOf(existing))
        val progressRepository = mockk<EntryProgressRepository>(relaxed = true)

        sync(
            source = source,
            repository = repository,
            progressRepository = progressRepository,
        )(entry(), fetchDetails = false)

        coVerify(exactly = 1) {
            progressRepository.rekey(
                entryId = 1L,
                chapterId = 1L,
                oldContentKey = "",
                oldResourceKey = "/old",
                newContentKey = "",
                newResourceKey = "/new",
            )
        }
    }

    @Test
    fun `blank chapter url rekeys progress from legacy fallback`() = runTest {
        val existing = chapter(id = 1L, url = "")
        val source = TestSource(chapters = listOf(sourceChapter(url = "/new")))
        val repository = chapterRepository(listOf(existing))
        val progressRepository = mockk<EntryProgressRepository>(relaxed = true)

        sync(
            source = source,
            repository = repository,
            progressRepository = progressRepository,
        )(entry(), fetchDetails = false)

        coVerify(exactly = 1) {
            progressRepository.rekey(
                entryId = 1L,
                chapterId = 1L,
                oldContentKey = "",
                oldResourceKey = "legacy-chapter:1",
                newContentKey = "",
                newResourceKey = "/new",
            )
        }
    }

    @Test
    fun `partial progress wins over an unstarted duplicate with the current url`() = runTest {
        val existing = listOf(
            chapter(id = 1L, url = "/old", sourceOrder = 0L),
            chapter(id = 2L, url = "/current", sourceOrder = 0L),
        )
        val source = TestSource(chapters = listOf(sourceChapter(url = "/current")))
        val repository = chapterRepository(existing)
        val removed = slot<List<Long>>()
        coEvery { repository.removeChaptersWithIds(capture(removed)) } returns Unit
        val progressRepository = mockk<EntryProgressRepository>(relaxed = true) {
            coEvery { getByEntryId(1L) } returns listOf(
                EntryProgressState(
                    entryId = 1L,
                    chapterId = 1L,
                    resourceKey = "/old",
                    locator = EntryProgressLocator(kind = "page", position = 4L, extent = 20L),
                    locatorUpdatedAt = 100L,
                ),
            )
        }

        sync(
            source = source,
            repository = repository,
            progressRepository = progressRepository,
        )(entry(), fetchDetails = false)

        removed.captured shouldBe listOf(2L)
        coVerify(exactly = 1) {
            progressRepository.rekey(
                entryId = 1L,
                chapterId = 1L,
                oldContentKey = "",
                oldResourceKey = "/old",
                newContentKey = "",
                newResourceKey = "/current",
            )
        }
    }

    @Test
    fun `backfilled chapter does not steal state when source urls also change`() = runTest {
        val existing = listOf(
            chapter(id = 1L, url = "/old-episode-1", name = "Episode 1", chapterNumber = 1.0, sourceOrder = 0L),
            chapter(
                id = 3L,
                url = "/old-episode-3",
                name = "Episode 3",
                chapterNumber = 2.0,
                sourceOrder = 1L,
                read = true,
            ),
        )
        val source = TestSource(
            chapters = listOf(
                sourceChapter(url = "/episode-1", name = "Episode 1", chapterNumber = 1.0),
                sourceChapter(url = "/episode-2", name = "Episode 2", chapterNumber = 2.0),
                sourceChapter(url = "/episode-3", name = "Episode 3", chapterNumber = 3.0),
            ),
        )
        val repository = chapterRepository(existing)
        val updates = slot<List<EntryChapter>>()
        val inserts = slot<List<EntryChapter>>()
        coEvery { repository.updateAll(capture(updates)) } returns true
        coEvery { repository.insertOrUpdate(capture(inserts)) } answers {
            inserts.captured.mapIndexed { index, chapter -> chapter.copy(id = 10L + index) }
        }

        sync(source, repository = repository)(entry(), fetchDetails = false)

        updates.captured.single { it.id == 3L }.run {
            url shouldBe "/episode-3"
            chapterNumber shouldBe 3.0
            read shouldBe true
        }
        inserts.captured.single().run {
            url shouldBe "/episode-2"
            read shouldBe false
        }
    }

    @Test
    fun `manual metadata refresh publishes persisted cover change while respecting title preference`() = runTest {
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
        val notifier = mockk<EntryMetadataChangeNotifier>(relaxed = true)

        sync(
            source = TestSource(details = details),
            entryRepository = entryRepository,
            metadataChangeNotifier = notifier,
            now = { 200L },
        )(entry, fetchChapters = false, manualFetch = true)

        updated.captured.title shouldBe "Stored title"
        updated.captured.coverLastModified shouldBe 200L
        coVerify(exactly = 1) { notifier.changed(entry, updated.captured) }
    }

    @Test
    fun `allowed source title change publishes metadata transition`() = runTest {
        val preferences = LibraryPreferences(InMemoryPreferenceStore()).also {
            it.updateMangaTitles.set(true)
        }
        val notifier = mockk<EntryMetadataChangeNotifier>(relaxed = true)
        val stored = entry().copy(favorite = true, title = "Stored title")
        val source = TestSource(details = sourceEntry(title = "Remote title"))

        sync(source, libraryPreferences = preferences, metadataChangeNotifier = notifier)(
            stored,
            fetchChapters = false,
        )

        coVerify(exactly = 1) { notifier.changed(stored, stored.copy(title = "Remote title", initialized = true)) }
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

    @Test
    fun `first chapter sync after library addition establishes updates baseline`() = runTest {
        val chapterRepository = chapterRepository(emptyList())
        val insertedChapters = slot<List<EntryChapter>>()
        coEvery { chapterRepository.insertOrUpdate(capture(insertedChapters)) } answers {
            insertedChapters.captured.map { it.copy(id = 10L) }
        }
        coEvery { chapterRepository.getChaptersByEntryIdAwait(1L, true) } answers {
            insertedChapters.captured.map { it.copy(id = 10L) }
        }
        val entryRepository = mockEntryRepository()
        val updatedEntries = mutableListOf<Entry>()
        coEvery { entryRepository.update(capture(updatedEntries)) } returns true

        sync(
            source = TestSource(chapters = listOf(sourceChapter())),
            repository = chapterRepository,
            entryRepository = entryRepository,
            now = { 1000L },
        )(
            entry().copy(favorite = true, dateAdded = 900L),
            fetchDetails = false,
        )

        insertedChapters.captured.single().dateFetch shouldBe 1000L
        updatedEntries.last().dateAdded shouldBe 1000L
    }

    @Test
    fun `first chapter after an earlier empty sync remains an update`() = runTest {
        val chapterRepository = chapterRepository(emptyList())
        val insertedChapters = slot<List<EntryChapter>>()
        coEvery { chapterRepository.insertOrUpdate(capture(insertedChapters)) } answers {
            insertedChapters.captured.map { it.copy(id = 10L) }
        }
        coEvery { chapterRepository.getChaptersByEntryIdAwait(1L, true) } answers {
            insertedChapters.captured.map { it.copy(id = 10L) }
        }
        val entryRepository = mockEntryRepository()
        val updatedEntries = mutableListOf<Entry>()
        coEvery { entryRepository.update(capture(updatedEntries)) } returns true

        sync(
            source = TestSource(chapters = listOf(sourceChapter())),
            repository = chapterRepository,
            entryRepository = entryRepository,
            now = { 1000L },
        )(
            entry().copy(favorite = true, dateAdded = 900L, fetchInterval = 7),
            fetchDetails = false,
        )

        updatedEntries.last().dateAdded shouldBe 900L
    }

    @Test
    fun `strict synchronization pins entry updates to the requested profile`() = runTest {
        val repository = mockEntryRepository()
        coEvery { repository.update(any(), 8L) } returns true
        val stored = entry().copy(profileId = 8L, title = "Stored")

        sync(
            source = TestSource(details = sourceEntry(title = "Remote")),
            entryRepository = repository,
        ).syncStrictly(
            entry = stored,
            profileId = 8L,
            updateLibraryTitles = true,
            fetchChapters = false,
        )

        coVerify(exactly = 1) { repository.update(match { it.title == "Remote" }, 8L) }
        coVerify(exactly = 0) { repository.update(any()) }
    }

    @Test
    fun `strict synchronization rejects swallowed chapter insertion failure`() = runTest {
        val chapters = chapterRepository(emptyList())
        coEvery { chapters.insertOrUpdate(any()) } returns emptyList()

        val error = runCatching {
            sync(
                source = TestSource(chapters = listOf(sourceChapter())),
                repository = chapters,
            ).syncStrictly(
                entry = entry().copy(profileId = 8L),
                profileId = 8L,
                updateLibraryTitles = false,
                fetchDetails = false,
            )
        }.exceptionOrNull()

        error.shouldBeInstanceOf<IllegalStateException>()
    }

    @Test
    fun `strict synchronization verifies requested chapter removals`() = runTest {
        val retained = chapter(id = 1L, url = "/old", sourceOrder = 0L, read = true)
        val removed = chapter(id = 2L, url = "/current", sourceOrder = 0L)
        val chapters = chapterRepository(listOf(retained, removed))
        coEvery { chapters.getChapterById(removed.id) } returns removed

        val error = runCatching {
            sync(
                source = TestSource(chapters = listOf(sourceChapter(url = "/current"))),
                repository = chapters,
            ).syncStrictly(
                entry = entry().copy(profileId = 8L),
                profileId = 8L,
                updateLibraryTitles = false,
                fetchDetails = false,
            )
        }.exceptionOrNull()

        error.shouldBeInstanceOf<IllegalStateException>()
    }

    private fun sync(
        source: UnifiedSource,
        repository: EntryChapterRepository = chapterRepository(emptyList()),
        progressRepository: EntryProgressRepository = mockk(relaxed = true),
        entryRepository: EntryRepository = mockEntryRepository(),
        libraryPreferences: LibraryPreferences = LibraryPreferences(InMemoryPreferenceStore()),
        fetchInterval: FetchInterval = mockFetchInterval(),
        metadataChangeNotifier: EntryMetadataChangeNotifier = mockk(relaxed = true),
        now: () -> Long = { 1000L },
    ): SyncEntryWithSource {
        val sourceManager = mockk<SourceManager> {
            every { get(1L) } returns source
        }
        return SyncEntryWithSource(
            entryRepository = entryRepository,
            entryChapterRepository = repository,
            entryProgressRepository = progressRepository,
            sourceManager = sourceManager,
            libraryPreferences = libraryPreferences,
            fetchInterval = fetchInterval,
            metadataChangeNotifier = metadataChangeNotifier,
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
        name: String = "Chapter 1",
        chapterNumber: Double = 1.0,
        sourceOrder: Long = 0L,
        read: Boolean = false,
        memo: kotlinx.serialization.json.JsonObject = buildJsonObject {},
    ): EntryChapter = EntryChapter.create().copy(
        id = id,
        entryId = 1L,
        url = url,
        name = name,
        chapterNumber = chapterNumber,
        sourceOrder = sourceOrder,
        read = read,
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
