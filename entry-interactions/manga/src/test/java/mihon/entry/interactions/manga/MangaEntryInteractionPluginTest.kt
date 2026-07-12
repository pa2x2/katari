package mihon.entry.interactions.manga

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.model.Page
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.EntryChildGroupFilterDataSource
import mihon.entry.interactions.EntryChildListRequest
import mihon.entry.interactions.EntryChildListRow
import mihon.entry.interactions.EntryChildProgressRequest
import mihon.entry.interactions.EntryDownloadState
import mihon.entry.interactions.EntryInteractionPlugin
import mihon.entry.interactions.EntryOpenOptions
import mihon.entry.interactions.EntryPreviewSize
import mihon.entry.interactions.createEntryInteractions
import mihon.entry.interactions.manga.download.DownloadCache
import mihon.entry.interactions.manga.download.DownloadManager
import mihon.entry.interactions.manga.download.model.DownloadState
import mihon.entry.interactions.manga.download.model.MangaDownload
import mihon.entry.interactions.settings.EntryInteractionPreferences
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.Preference
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR

class MangaEntryInteractionPluginTest {
    private val context = mockk<Context>(relaxed = true)

    @Test
    fun `plugin registers manga processors`() = runTest {
        val dependencies = dependencies(
            chapters = listOf(chapter(id = 10L, read = false)),
            chapterDownloaded = true,
        )
        val interactions = createEntryInteractions(listOf(mangaEntryInteractionPlugin(dependencies)))
        val entry = entry(EntryType.MANGA, id = 1L)

        val continued = interactions.continueEntry.findNext(entry)
        val status = interactions.download.getStatus(
            entryType = EntryType.MANGA,
            chapterId = 10L,
            chapterName = "Chapter",
            chapterScanlator = null,
            chapterUrl = "/chapter",
            entryTitle = "Entry",
            sourceId = 1L,
        )

        continued?.id shouldBe 10L
        status.state shouldBe EntryDownloadState.DOWNLOADED
    }

    @Test
    fun `manga library progress preserves last read timestamp`() = runTest {
        val state = mangaEntryLibraryProgressCalculator().calculate(
            entry = entry(EntryType.MANGA),
            chapters = listOf(chapter(read = true)),
            lastRead = 1234L,
        )

        state.lastRead shouldBe 1234L
    }

    @Test
    fun `manga child list preserves missing chapter insertion`() = runTest {
        val interactions = createEntryInteractions(listOf(mangaEntryInteractionPlugin(dependencies())))
        val entry = entry(EntryType.MANGA).copy(
            chapterFlags = Entry.CHAPTER_SORTING_NUMBER or Entry.CHAPTER_SORT_ASC,
        )
        val rows = interactions.childList.buildDisplayList(
            EntryChildListRequest(
                entry = entry,
                chapters = listOf(
                    chapter(id = 3L, chapterNumber = 3.0),
                    chapter(id = 1L, chapterNumber = 1.0),
                ),
                memberIds = listOf(entry.id),
                includeMissingCounts = true,
            ),
        )

        rows.map { row ->
            when (row) {
                is EntryChildListRow.Child -> "child:${row.chapter.id}"
                is EntryChildListRow.MissingCount -> "missing:${row.id}:${row.count}"
                is EntryChildListRow.MemberHeader -> "header:${row.entryId}"
            }
        }.shouldContainExactly(
            "child:1",
            "missing:1-3:1",
            "child:3",
        )
    }

    @Test
    fun `manga child list sorts chapter number descending with largest number first`() = runTest {
        val interactions = createEntryInteractions(listOf(mangaEntryInteractionPlugin(dependencies())))
        val entry = entry(EntryType.MANGA).copy(
            chapterFlags = Entry.CHAPTER_SORTING_NUMBER or Entry.CHAPTER_SORT_DESC,
        )
        val rows = interactions.childList.buildDisplayList(
            EntryChildListRequest(
                entry = entry,
                chapters = listOf(
                    chapter(id = 1L, chapterNumber = 1.0),
                    chapter(id = 3L, chapterNumber = 3.0),
                    chapter(id = 2L, chapterNumber = 2.0),
                ),
                memberIds = listOf(entry.id),
                includeMissingCounts = true,
            ),
        )

        rows.filterIsInstance<EntryChildListRow.Child>()
            .map { it.chapter.id }
            .shouldContainExactly(3L, 2L, 1L)
    }

    @Test
    fun `partial unread manga chapter returns chapter progress label`() = runTest {
        val interactions = createEntryInteractions(listOf(mangaEntryInteractionPlugin(dependencies())))

        val labels = interactions.childList.progressLabels(
            EntryChildProgressRequest(
                entry = entry(EntryType.MANGA),
                chapters = listOf(chapter(id = 7L, read = false, lastPageRead = 4L)),
            ),
        ).first()

        labels[7L]?.resource shouldBe MR.strings.chapter_progress
        labels[7L]?.args shouldBe listOf(5L)
    }

    @Test
    fun `read manga chapter returns no progress label`() = runTest {
        val interactions = createEntryInteractions(listOf(mangaEntryInteractionPlugin(dependencies())))

        val labels = interactions.childList.progressLabels(
            EntryChildProgressRequest(
                entry = entry(EntryType.MANGA),
                chapters = listOf(chapter(id = 7L, read = true, lastPageRead = 4L)),
            ),
        ).first()

        labels shouldBe emptyMap()
    }

    @Test
    fun `manga processors reject anime entries`() = runTest {
        val dependencies = dependencies()
        val openProcessor = MangaOpenProcessor()
        val continueProcessor = MangaContinueProcessor(dependencies.getEntryWithChapters, openProcessor)
        val downloadProcessor = MangaDownloadProcessor(dependencies)
        val consumptionProcessor = MangaConsumptionProcessor(
            entryChapterRepository = dependencies.entryChapterRepository,
            downloadPreferences = dependencies.downloadPreferences,
            downloadManager = dependencies.downloadManager,
            sourceManager = dependencies.sourceManager,
        )
        val animeEntry = entry(EntryType.ANIME)

        val openError = assertFailsWith<IllegalArgumentException> {
            openProcessor.open(context, animeEntry, chapter(), EntryOpenOptions())
        }
        val continueError = assertFailsWith<IllegalArgumentException> {
            continueProcessor.findNext(animeEntry)
        }
        val downloadError = assertFailsWith<IllegalArgumentException> {
            downloadProcessor.download(animeEntry, listOf(chapter()), startNow = false)
        }
        val consumptionError = assertFailsWith<IllegalArgumentException> {
            consumptionProcessor.setConsumed(animeEntry, listOf(chapter()), consumed = true)
        }

        openError.message shouldContain "expected MANGA"
        continueError.message shouldContain "expected MANGA"
        downloadError.message shouldContain "expected MANGA"
        consumptionError.message shouldContain "expected MANGA"
    }

    @Test
    fun `manga continue selects next unread chapter`() = runTest {
        val nextUnread = chapter(id = 3L, read = false, sourceOrder = 2L)
        val dependencies = dependencies(
            chapters = listOf(
                chapter(id = 1L, read = true, sourceOrder = 0L),
                chapter(id = 2L, read = false, sourceOrder = 4L),
                nextUnread,
            ),
        )
        val processor = MangaContinueProcessor(
            getEntryWithChapters = dependencies.getEntryWithChapters,
            openProcessor = MangaOpenProcessor(),
        )

        val result = processor.findNext(entry(EntryType.MANGA, id = 1L))

        result shouldBe nextUnread
    }

    @Test
    fun `manga continue selects unread chapter from merged member`() = runTest {
        val siblingChapter = chapter(id = 3L, entryId = 2L, read = false)
        val getEntryWithChapters = mockk<GetEntryWithChapters> {
            coEvery { awaitChapters(1L) } returns listOf(
                chapter(id = 1L, entryId = 1L, read = true),
                siblingChapter,
            )
        }
        val processor = MangaContinueProcessor(getEntryWithChapters, MangaOpenProcessor())

        val result = processor.findNext(entry(EntryType.MANGA, id = 1L))

        result shouldBe siblingChapter
    }

    @Test
    fun `facade continue opens through manga open processor`() = runTest {
        val opened = mutableListOf<Pair<Long, Long>>()
        val dependencies = dependencies(
            chapters = listOf(chapter(id = 22L, entryId = 7L, read = false)),
        )
        val interactions = createEntryInteractions(
            listOf(
                EntryInteractionPlugin { registry ->
                    val openProcessor = MangaOpenProcessor(openChapter = { _, entry, chapter, _ ->
                        opened += entry.id to chapter.id
                    })
                    registry.registerContinueProcessor(
                        MangaContinueProcessor(
                            getEntryWithChapters = dependencies.getEntryWithChapters,
                            openProcessor = openProcessor,
                        ),
                    )
                },
            ),
        )

        val result = interactions.continueEntry.continueEntry(context, entry(EntryType.MANGA, id = 7L))

        result?.id shouldBe 22L
        opened.shouldContainExactly(7L to 22L)
    }

    @Test
    fun `facade continue does not open when no unread chapter exists`() = runTest {
        val interactions = createEntryInteractions(
            listOf(
                mangaEntryInteractionPlugin(
                    dependencies(
                        chapters = listOf(chapter(id = 1L, read = true)),
                    ),
                ),
            ),
        )

        val result = interactions.continueEntry.continueEntry(context, entry(EntryType.MANGA))

        result.shouldBeNull()
    }

    @Test
    fun `manga download state mapping maps real runtime states`() {
        DownloadState.NOT_DOWNLOADED.toEntryDownloadState() shouldBe EntryDownloadState.NOT_DOWNLOADED
        DownloadState.QUEUE.toEntryDownloadState() shouldBe EntryDownloadState.QUEUE
        DownloadState.DOWNLOADING.toEntryDownloadState() shouldBe EntryDownloadState.DOWNLOADING
        DownloadState.DOWNLOADED.toEntryDownloadState() shouldBe EntryDownloadState.DOWNLOADED
        DownloadState.ERROR.toEntryDownloadState() shouldBe EntryDownloadState.ERROR
    }

    @Test
    fun `manga consumption marks read`() = runTest {
        val repository = FakeEntryChapterRepository(
            listOf(
                chapter(id = 1L, read = false),
                chapter(id = 2L, read = true),
            ),
        )
        val processor = mangaConsumptionProcessor(repository)

        processor.setConsumed(
            entry = entry(EntryType.MANGA),
            chapters = listOf(
                chapter(id = 1L, read = false),
                chapter(id = 2L, read = true),
            ),
            consumed = true,
        )

        repository.updatedChapters.shouldContainExactly(
            chapter(id = 1L, read = true),
        )
    }

    @Test
    fun `manga consumption marks unread and resets progress`() = runTest {
        val repository = FakeEntryChapterRepository(
            listOf(
                chapter(id = 1L, read = true, lastPageRead = 5L),
                chapter(id = 2L, read = false, lastPageRead = 4L),
            ),
        )
        val processor = mangaConsumptionProcessor(repository)

        processor.setConsumed(
            entry = entry(EntryType.MANGA),
            chapters = listOf(
                chapter(id = 1L, read = true, lastPageRead = 5L),
                chapter(id = 2L, read = false, lastPageRead = 4L),
            ),
            consumed = false,
        )

        repository.updatedChapters.shouldContainExactly(
            chapter(id = 1L, read = false, lastPageRead = 0L),
            chapter(id = 2L, read = false, lastPageRead = 0L),
        )
    }

    @Test
    fun `manga consumption deletes downloads only when marking read and preference is enabled`() = runTest {
        val repository = FakeEntryChapterRepository(emptyList())
        val downloadManager = mockDownloadManager(chapterDownloaded = false)
        val sourceManager = mockSourceManager()
        val processor = mangaConsumptionProcessor(
            repository = repository,
            removeAfterMarkedAsRead = true,
            downloadManager = downloadManager,
            sourceManager = sourceManager,
        )
        val entry = entry(EntryType.MANGA)
        val chapter = chapter(id = 1L, read = false)

        processor.setConsumed(entry, listOf(chapter), consumed = true)
        processor.setConsumed(entry, listOf(chapter.copy(read = true, lastPageRead = 5L)), consumed = false)

        verify(exactly = 1) {
            downloadManager.deleteChapters(
                listOf(chapter),
                entry,
                any(),
            )
        }
    }

    @Test
    fun `manga consumption does not delete downloads when preference is disabled`() = runTest {
        val repository = FakeEntryChapterRepository(emptyList())
        val downloadManager = mockDownloadManager(chapterDownloaded = false)
        val processor = mangaConsumptionProcessor(
            repository = repository,
            removeAfterMarkedAsRead = false,
            downloadManager = downloadManager,
        )
        val entry = entry(EntryType.MANGA)

        processor.setConsumed(entry, listOf(chapter(id = 1L, read = false)), consumed = true)

        verify(exactly = 0) { downloadManager.deleteChapters(any(), any(), any()) }
    }

    @Test
    fun `manga consumption updates bookmarks`() = runTest {
        val repository = FakeEntryChapterRepository(
            listOf(
                chapter(id = 1L, bookmark = false),
                chapter(id = 2L, bookmark = true),
            ),
        )
        val processor = mangaConsumptionProcessor(repository)

        processor.setBookmarked(
            entry = entry(EntryType.MANGA),
            chapters = listOf(
                chapter(id = 1L, bookmark = false),
                chapter(id = 2L, bookmark = true),
            ),
            bookmarked = true,
        )

        repository.updatedChapters.shouldContainExactly(
            chapter(id = 1L, bookmark = true),
        )
    }

    @Test
    fun `manga download model maps to entry status and queue item`() {
        val download = MangaDownload(
            source = source(id = 2L, name = "Source"),
            entry = entry(EntryType.MANGA, id = 7L, title = "Entry"),
            chapter = chapter(id = 9L, name = "Chapter 9", dateUpload = 123L, chapterNumber = 9.0),
        ).apply {
            status = DownloadState.DOWNLOADING
            pages = listOf(
                Page(0).apply {
                    status = Page.State.Ready
                    progress = 100
                },
                Page(1).apply {
                    progress = 50
                },
            )
        }

        val status = download.toEntryDownloadStatus()
        val item = download.toEntryDownloadQueueItem()
        val groups = listOf(download).toMangaEntryDownloadQueueGroups()

        status.entryType shouldBe EntryType.MANGA
        status.chapterId shouldBe 9L
        status.state shouldBe EntryDownloadState.DOWNLOADING
        status.progress shouldBe 75
        item.entryId shouldBe 7L
        item.childId shouldBe 9L
        item.title shouldBe "Entry"
        item.subtitle shouldBe "Chapter 9"
        item.progress shouldBe 150
        item.progressMax shouldBe 200
        item.progressText shouldBe "1/2"
        groups.map { it.sourceName }.shouldContainExactly("Source")
    }

    @Test
    fun `manga preview config follows manga preview preferences`() = runTest {
        val entryInteractionPreferences = EntryInteractionPreferences(InMemoryPreferenceStore())
        entryInteractionPreferences.enableMangaPreview.set(true)
        entryInteractionPreferences.mangaPreviewPageCount.set(12)
        entryInteractionPreferences.mangaPreviewSize.set(EntryPreviewSize.LARGE)
        val interactions = createEntryInteractions(
            listOf(
                mangaEntryInteractionPlugin(
                    dependencies(entryInteractionPreferences = entryInteractionPreferences),
                ),
            ),
        )

        val config = interactions.preview.config(entry(EntryType.MANGA))

        config.enabled shouldBe true
        config.pageCount shouldBe 12
        config.size shouldBe EntryPreviewSize.LARGE
    }

    private fun dependencies(
        chapters: List<EntryChapter> = emptyList(),
        chapterDownloaded: Boolean = false,
        entryInteractionPreferences: EntryInteractionPreferences =
            EntryInteractionPreferences(InMemoryPreferenceStore()),
    ): MangaEntryInteractionRuntimeDependencies {
        return MangaEntryInteractionRuntimeDependencies(
            getEntryWithChapters = mockk {
                coEvery { awaitChapters(any()) } returns chapters.sortedBy { it.sourceOrder }
            },
            entryChapterRepository = FakeEntryChapterRepository(chapters),
            filterEntryChaptersForDownload = mockk(relaxed = true),
            childGroupFilterDataSource = FakeEntryChildGroupFilterDataSource(),
            downloadPreferences = mockDownloadPreferences(),
            downloadManager = mockDownloadManager(chapterDownloaded),
            downloadCache = mockDownloadCache(),
            sourceManager = mockSourceManager(),
            entryInteractionPreferences = entryInteractionPreferences,
        )
    }

    private fun mangaConsumptionProcessor(
        repository: EntryChapterRepository,
        removeAfterMarkedAsRead: Boolean = false,
        downloadManager: DownloadManager = mockDownloadManager(chapterDownloaded = false),
        sourceManager: SourceManager = mockSourceManager(),
    ): MangaConsumptionProcessor {
        return MangaConsumptionProcessor(
            entryChapterRepository = repository,
            downloadPreferences = mockDownloadPreferences(removeAfterMarkedAsRead),
            downloadManager = downloadManager,
            sourceManager = sourceManager,
        )
    }

    private fun mockDownloadPreferences(removeAfterMarkedAsRead: Boolean = false): DownloadPreferences {
        val preference = mockk<Preference<Boolean>> {
            every { this@mockk.get() } returns removeAfterMarkedAsRead
        }
        return mockk(relaxed = true) {
            every { this@mockk.removeAfterMarkedAsRead } returns preference
        }
    }

    private fun mockDownloadManager(chapterDownloaded: Boolean): DownloadManager {
        val queueState = MutableStateFlow<List<MangaDownload>>(emptyList())
        return mockk(relaxed = true) {
            every { this@mockk.queueState } returns queueState
            every { this@mockk.isDownloaderRunning } returns flowOf(false)
            every { this@mockk.statusFlow() } returns emptyFlow()
            every { this@mockk.progressFlow() } returns emptyFlow()
            every { this@mockk.getQueuedDownloadOrNull(any()) } returns null
            every { this@mockk.isChapterDownloaded(any(), any(), any(), any(), any(), any()) } returns chapterDownloaded
            every { this@mockk.getDownloadCount(any<Entry>()) } returns 0
            every { this@mockk.getDownloadCount() } returns 0
        }
    }

    private fun mockDownloadCache(): DownloadCache {
        return mockk(relaxed = true) {
            every { this@mockk.changes } returns MutableSharedFlow<Unit>()
            every { this@mockk.isInitializing } returns MutableStateFlow(false)
        }
    }

    private fun mockSourceManager(): SourceManager {
        val source = source()
        return mockk(relaxed = true) {
            every { this@mockk.get(any()) } returns source
            every { this@mockk.getOrStub(any()) } returns source
        }
    }

    private fun source(id: Long = 1L, name: String = "Source"): UnifiedSource {
        return mockk {
            every { this@mockk.id } returns id
            every { this@mockk.name } returns name
        }
    }

    private fun entry(type: EntryType, id: Long = 1L, title: String = "Entry"): Entry {
        return Entry.create().copy(id = id, title = title, type = type)
    }

    private fun chapter(
        id: Long = 1L,
        entryId: Long = 1L,
        name: String = "Chapter",
        read: Boolean = false,
        bookmark: Boolean = false,
        lastPageRead: Long = 0L,
        sourceOrder: Long = 0L,
        dateUpload: Long = 0L,
        chapterNumber: Double = 0.0,
    ): EntryChapter {
        return EntryChapter.create().copy(
            id = id,
            entryId = entryId,
            name = name,
            read = read,
            bookmark = bookmark,
            lastPageRead = lastPageRead,
            sourceOrder = sourceOrder,
            dateUpload = dateUpload,
            chapterNumber = chapterNumber,
        )
    }

    private class FakeEntryChapterRepository(
        private val chapters: List<EntryChapter>,
    ) : EntryChapterRepository {
        val updatedChapters = mutableListOf<EntryChapter>()

        override suspend fun getChapterById(id: Long): EntryChapter? = chapters.firstOrNull { it.id == id }

        override fun getChaptersByEntryId(entryId: Long): Flow<List<EntryChapter>> {
            return flowOf(chapters.filter { it.entryId == entryId })
        }

        override fun getChaptersByEntryIds(entryIds: List<Long>): Flow<List<EntryChapter>> {
            return flowOf(chapters.filter { it.entryId in entryIds })
        }

        override suspend fun getChaptersByEntryIdAwait(
            entryId: Long,
            applyScanlatorFilter: Boolean,
        ): List<EntryChapter> {
            return chapters.filter { it.entryId == entryId }
        }

        override suspend fun getRecentRead(offset: Int, limit: Int): List<EntryChapter> = emptyList()

        override suspend fun getBookmarkedChaptersByEntryId(entryId: Long): List<EntryChapter> {
            return chapters.filter { it.entryId == entryId && it.bookmark }
        }

        override suspend fun insert(chapter: EntryChapter): Long = chapter.id

        override suspend fun insertOrUpdate(chapters: List<EntryChapter>): List<EntryChapter> = chapters

        override suspend fun update(chapter: EntryChapter): Boolean = true

        override suspend fun updateAll(chapters: List<EntryChapter>): Boolean {
            updatedChapters += chapters
            return true
        }

        override suspend fun delete(id: Long): Boolean = true

        override suspend fun deleteByEntryId(entryId: Long): Boolean = true

        override suspend fun removeChaptersWithIds(chapterIds: List<Long>) = Unit

        override suspend fun getScanlatorsByEntryId(entryId: Long): List<String> = emptyList()

        override fun getScanlatorsByEntryIdAsFlow(entryId: Long): Flow<List<String>> = flowOf(emptyList())

        override suspend fun getChapterByUrlAndEntryId(url: String, entryId: Long): EntryChapter? {
            return chapters.firstOrNull { it.url == url && it.entryId == entryId }
        }
    }

    private class FakeEntryChildGroupFilterDataSource : EntryChildGroupFilterDataSource {
        override fun availableGroupsChanged(entryId: Long): Flow<Unit> = emptyFlow()

        override suspend fun availableGroups(entryIds: Collection<Long>): Set<String> = emptySet()

        override fun excludedGroupsChanged(entryId: Long): Flow<Unit> = emptyFlow()

        override suspend fun excludedGroups(entryIds: Collection<Long>): Set<String> = emptySet()

        override suspend fun setExcludedGroups(entryIds: Collection<Long>, excluded: Set<String>) = Unit
    }

    private suspend inline fun <reified T : Throwable> assertFailsWith(
        crossinline block: suspend () -> Unit,
    ): T {
        try {
            block()
        } catch (throwable: Throwable) {
            if (throwable is T) {
                return throwable
            }
            throw throwable
        }
        error("Expected ${T::class.simpleName} to be thrown")
    }
}
