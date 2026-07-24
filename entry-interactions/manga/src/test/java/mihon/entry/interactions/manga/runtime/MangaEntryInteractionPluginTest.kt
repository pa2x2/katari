package mihon.entry.interactions.manga

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.model.Page
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
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
import mihon.entry.interactions.EntryChildListRequest
import mihon.entry.interactions.EntryChildListRow
import mihon.entry.interactions.EntryChildProgressRequest
import mihon.entry.interactions.EntryDownloadPhase
import mihon.entry.interactions.EntryDownloadProgress
import mihon.entry.interactions.EntryDownloadState
import mihon.entry.interactions.EntryMediaSessionEventSink
import mihon.entry.interactions.EntryMediaSessionResult
import mihon.entry.interactions.EntryOpenOptions
import mihon.entry.interactions.EntryPreviewSize
import mihon.entry.interactions.EntryProgressResourceMapping
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
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR

class MangaEntryInteractionPluginTest {
    private val context = mockk<Context>(relaxed = true)

    @Test
    fun `manga library progress provider exposes partial page evidence`() = runTest {
        val evidence = MangaLibraryProgressProvider(
            FakeEntryProgressRepository(
                listOf(pageProgress(chapterId = 2L, pageIndex = 4L, updatedAt = 2_000L)),
            ),
        ).evidence(
            entry = entry(EntryType.MANGA),
            chapters = listOf(chapter(id = 1L), chapter(id = 2L)),
        )

        evidence.hasMediaProgress shouldBe true
        evidence.inProgressItemId shouldBe 2L
        evidence.inProgressFraction shouldBe (4f / 9f)
        evidence.lastActivityAt shouldBe 2_000L
    }

    @Test
    fun `manga child list preserves missing chapter insertion`() = runTest {
        val interactions = createEntryInteractions(
            listOf(
                mangaEntryInteractionPlugin(
                    dependencies(progressStates = listOf(pageProgress(chapterId = 7L, pageIndex = 4L))),
                ),
            ),
        )
        val entry = entry(EntryType.MANGA).copy(
            chapterFlags = Entry.CHAPTER_SORTING_NUMBER or Entry.CHAPTER_SORT_ASC,
        )
        val rows = interactions.missingChildGap.buildDisplayList(
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

        rows.rows.map { row ->
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
    fun `manga child groups use non-blank normalized scanlator names`() {
        val entry = entry(EntryType.MANGA)

        MangaChildGroupFilterProcessor.groupFor(entry, chapter().copy(scanlator = " Group A ")) shouldBe "Group A"
        MangaChildGroupFilterProcessor.groupFor(entry, chapter().copy(scanlator = "  ")).shouldBeNull()
    }

    @Test
    fun `manga child list sorts chapter number descending with largest number first`() = runTest {
        val interactions = createEntryInteractions(listOf(mangaEntryInteractionPlugin(dependencies())))
        val entry = entry(EntryType.MANGA).copy(
            chapterFlags = Entry.CHAPTER_SORTING_NUMBER or Entry.CHAPTER_SORT_DESC,
        )
        val rows = interactions.childList.sortedForDisplay(
            entry = entry,
            chapters = listOf(
                chapter(id = 1L, chapterNumber = 1.0),
                chapter(id = 3L, chapterNumber = 3.0),
                chapter(id = 2L, chapterNumber = 2.0),
            ),
            memberIds = listOf(entry.id),
        )

        rows
            .map { it.id }
            .shouldContainExactly(3L, 2L, 1L)
    }

    @Test
    fun `partial unread manga chapter returns chapter progress label`() = runTest {
        val interactions = createEntryInteractions(
            listOf(
                mangaEntryInteractionPlugin(
                    dependencies(progressStates = listOf(pageProgress(chapterId = 7L, pageIndex = 4L))),
                ),
            ),
        )

        val labels = interactions.childProgress.progressLabels(
            EntryChildProgressRequest(
                entry = entry(EntryType.MANGA),
                chapters = listOf(chapter(id = 7L, read = false)),
                memberIds = listOf(1L),
            ),
        ).first()

        labels[7L]?.resource shouldBe MR.strings.chapter_progress
        labels[7L]?.args shouldBe listOf(5L)
    }

    @Test
    fun `read manga chapter returns no progress label`() = runTest {
        val interactions = createEntryInteractions(
            listOf(
                mangaEntryInteractionPlugin(
                    dependencies(progressStates = listOf(pageProgress(chapterId = 7L, pageIndex = 4L))),
                ),
            ),
        )

        val labels = interactions.childProgress.progressLabels(
            EntryChildProgressRequest(
                entry = entry(EntryType.MANGA),
                chapters = listOf(chapter(id = 7L, read = true)),
                memberIds = listOf(1L),
            ),
        ).first()

        labels shouldBe emptyMap()
    }

    @Test
    fun `manga progress copy maps page state to target resource`() = runTest {
        val progressRepository = FakeEntryProgressRepository(
            listOf(pageProgress(entryId = 1L, chapterId = 10L, pageIndex = 4L)),
        )
        val processor = MangaProgressProcessor(progressRepository, FakeEntryChapterRepository(emptyList()))

        processor.copy(
            sourceEntry = entry(EntryType.MANGA, id = 1L),
            targetEntry = entry(EntryType.MANGA, id = 2L),
            resourceMappings = listOf(
                EntryProgressResourceMapping(
                    sourceResourceKey = "/chapter/10",
                    targetResourceKey = "/chapter/20",
                    targetChapterId = 20L,
                ),
            ),
        )

        progressRepository.upsertedStates.shouldContainExactly(
            pageProgress(entryId = 2L, chapterId = 20L, pageIndex = 4L),
        )
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
            entryProgressRepository = dependencies.entryProgressRepository,
            openProcessor = MangaOpenProcessor(),
        )

        val result = processor.findNext(entry(EntryType.MANGA, id = 1L))

        result shouldBe nextUnread
    }

    @Test
    fun `manga continue selects unread chapter from merged member`() = runTest {
        val siblingChapter = chapter(id = 3L, entryId = 2L, read = false)
        val getEntryWithChapters = mockk<GetEntryWithChapters> {
            coEvery { awaitChapters(any()) } returns listOf(
                chapter(id = 1L, entryId = 1L, read = true),
                siblingChapter,
            )
        }
        val processor = MangaContinueProcessor(
            getEntryWithChapters,
            FakeEntryProgressRepository(emptyList()),
            MangaOpenProcessor(),
        )

        val result = processor.findNext(entry(EntryType.MANGA, id = 1L))

        result shouldBe siblingChapter
    }

    @Test
    fun `manga continue prefers most recently updated partial chapter from merged member`() = runTest {
        val rootChapter = chapter(id = 1L, entryId = 1L, read = false)
        val siblingChapter = chapter(id = 3L, entryId = 2L, read = false)
        val getEntryWithChapters = mockk<GetEntryWithChapters> {
            coEvery { awaitChapters(any()) } returns listOf(rootChapter, siblingChapter)
        }
        val progressRepository = FakeEntryProgressRepository(
            listOf(
                pageProgress(entryId = 1L, chapterId = 1L, pageIndex = 2L, updatedAt = 10L),
                pageProgress(entryId = 2L, chapterId = 3L, pageIndex = 4L, updatedAt = 20L),
            ),
        )
        val processor = MangaContinueProcessor(
            getEntryWithChapters,
            progressRepository,
            MangaOpenProcessor(),
        )

        val result = processor.findNext(entry(EntryType.MANGA, id = 1L))

        result shouldBe siblingChapter
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
    fun `manga consumption marks read without changing recency`() = runTest {
        val repository = FakeEntryChapterRepository(
            listOf(
                chapter(id = 1L, read = false),
                chapter(id = 2L, read = true),
            ),
        )
        val progressRepository = FakeEntryProgressRepository(emptyList())
        val processor = mangaConsumptionProcessor(repository, progressRepository = progressRepository)

        processor.setConsumed(
            entry = entry(EntryType.MANGA),
            chapters = listOf(
                chapter(id = 1L, read = false),
                chapter(id = 2L, read = true),
            ),
            consumed = true,
        )

        progressRepository.upsertedStates.map { Triple(it.chapterId, it.completed, it.lastReadAt) }
            .shouldContainExactly(Triple(1L, true, 0L))
    }

    @Test
    fun `manga consumption uses legacy progress key for blank chapter url`() = runTest {
        val target = chapter(id = 7L, url = "")
        val repository = FakeEntryChapterRepository(listOf(target))
        val progressRepository = FakeEntryProgressRepository(emptyList())
        val processor = mangaConsumptionProcessor(repository, progressRepository = progressRepository)

        processor.setConsumed(entry(EntryType.MANGA), listOf(target), consumed = true)

        progressRepository.upsertedStates.single().resourceKey shouldBe "legacy-chapter:7"
    }

    @Test
    fun `manga consumption marks unread and resets progress without changing recency`() = runTest {
        val repository = FakeEntryChapterRepository(
            listOf(
                chapter(id = 1L, read = true),
                chapter(id = 2L, read = false),
            ),
        )
        val progressRepository = FakeEntryProgressRepository(
            listOf(
                pageProgress(chapterId = 1L, pageIndex = 5L, completed = true),
                pageProgress(chapterId = 2L, pageIndex = 4L),
            ),
        )
        val processor = mangaConsumptionProcessor(repository, progressRepository = progressRepository)

        processor.setConsumed(
            entry = entry(EntryType.MANGA),
            chapters = listOf(
                chapter(id = 1L, read = true),
                chapter(id = 2L, read = false),
            ),
            consumed = false,
        )

        progressRepository.upsertedStates.map {
            listOf(it.chapterId, it.pageIndex, it.completed, it.locatorUpdatedAt, it.completionUpdatedAt)
        }
            .shouldContainExactly(
                listOf(1L, 0L, false, 1L, 1L),
                listOf(2L, 0L, false, 1L, 1L),
            )
    }

    @Test
    fun `manga consumption returns exactly the newly consumed children`() = runTest {
        val repository = FakeEntryChapterRepository(emptyList())
        val processor = mangaConsumptionProcessor(repository = repository)
        val entry = entry(EntryType.MANGA)
        val chapter = chapter(id = 1L, read = false)

        val changed = processor.setConsumed(
            entry,
            listOf(chapter, chapter(id = 2L, read = true)),
            consumed = true,
        )

        changed.shouldContainExactly(chapter)
    }

    @Test
    fun `manga consumption returns no children when state does not change`() = runTest {
        val repository = FakeEntryChapterRepository(emptyList())
        val processor = mangaConsumptionProcessor(repository = repository)
        val entry = entry(EntryType.MANGA)

        val changed = processor.setConsumed(
            entry,
            listOf(chapter(id = 1L, read = true)),
            consumed = true,
        )

        changed shouldBe emptyList()
    }

    @Test
    fun `manga bookmark provider persists supplied mutations`() = runTest {
        val repository = FakeEntryChapterRepository(
            listOf(
                chapter(id = 1L, bookmark = false),
                chapter(id = 2L, bookmark = true),
            ),
        )
        val processor = mangaConsumptionProcessor(repository)

        processor.setBookmarked(
            entry = entry(EntryType.MANGA),
            chapters = listOf(chapter(id = 1L, bookmark = false)),
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
            entry = entry(EntryType.MANGA, id = 7L, title = "Entry", sourceId = 2L),
            chapter = chapter(id = 9L, entryId = 7L, name = "Chapter 9", dateUpload = 123L, chapterNumber = 9.0),
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
        item.presentation.phase shouldBe EntryDownloadPhase.TRANSFERRING
        item.presentation.progress shouldBe EntryDownloadProgress.Units(completed = 1, total = 2)
        groups.map { it.sourceName }.shouldContainExactly("Source")
    }

    @Test
    fun `manga downloads start normally and promote every selected chapter for start now`() = runTest {
        val manager = mockDownloadManager(chapterDownloaded = false)
        val interactions = createEntryInteractions(
            listOf(mangaEntryInteractionPlugin(dependencies(downloadManager = manager))),
        )
        val manga = entry(EntryType.MANGA)
        val chapters = listOf(chapter(id = 2L), chapter(id = 3L))

        interactions.download.download(manga, chapters, startNow = false)

        verify(exactly = 1) { manager.downloadChapters(manga, chapters, autoStart = false) }
        verify(exactly = 1) { manager.startDownloads() }

        interactions.download.download(manga, chapters, startNow = true)

        verify(exactly = 2) { manager.downloadChapters(manga, chapters, autoStart = false) }
        verify(exactly = 1) { manager.startDownloadsNow(listOf(2L, 3L)) }
    }

    @Test
    fun `manga lifecycle cleanup is deferred through the pending deletion store`() = runTest {
        val manager = mockDownloadManager(chapterDownloaded = true)
        val interactions = createEntryInteractions(
            listOf(mangaEntryInteractionPlugin(dependencies(downloadManager = manager))),
        )
        val manga = entry(EntryType.MANGA)
        val chapter = chapter()

        interactions.download.cleanup(manga, listOf(chapter))

        coVerify(exactly = 1) { manager.enqueueChaptersToDelete(listOf(chapter), manga) }
        coVerify(exactly = 0) { manager.deleteChapters(any(), any(), any()) }
    }

    @Test
    fun `manga merged downloads are queued under each real owner and start once`() = runTest {
        val visible = entry(EntryType.MANGA, id = 1L, sourceId = 10L, profileId = 7L)
        val member = entry(EntryType.MANGA, id = 2L, sourceId = 20L, profileId = 7L)
        val visibleChapter = chapter(id = 11L, entryId = visible.id)
        val memberChapter = chapter(id = 21L, entryId = member.id)
        val manager = mockDownloadManager(chapterDownloaded = false)
        val interactions = createEntryInteractions(
            listOf(
                mangaEntryInteractionPlugin(
                    dependencies(downloadManager = manager, entries = listOf(member)),
                ),
            ),
        )

        interactions.download.download(visible, listOf(visibleChapter, memberChapter), startNow = false)

        verify(exactly = 1) {
            manager.downloadChapters(visible, listOf(visibleChapter), autoStart = false)
            manager.downloadChapters(member, listOf(memberChapter), autoStart = false)
            manager.startDownloads()
        }
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

        val config = requireNotNull(interactions.preview.configuration(EntryType.MANGA)).config()

        config.enabled shouldBe true
        config.pageCount shouldBe 12
        config.size shouldBe EntryPreviewSize.LARGE
    }

    private fun dependencies(
        chapters: List<EntryChapter> = emptyList(),
        progressStates: List<EntryProgressState> = emptyList(),
        chapterDownloaded: Boolean = false,
        downloadManager: DownloadManager = mockDownloadManager(chapterDownloaded),
        entries: List<Entry> = emptyList(),
        entryInteractionPreferences: EntryInteractionPreferences =
            EntryInteractionPreferences(InMemoryPreferenceStore()),
    ): MangaEntryInteractionRuntimeDependencies {
        return MangaEntryInteractionRuntimeDependencies(
            getEntryWithChapters = mockk {
                coEvery { awaitChapters(any()) } returns chapters.sortedBy { it.sourceOrder }
            },
            entryChapterRepository = FakeEntryChapterRepository(chapters),
            entryProgressRepository = FakeEntryProgressRepository(progressStates),
            downloadPreferences = mockDownloadPreferences(),
            downloadManager = downloadManager,
            downloadCache = mockDownloadCache(),
            sourceManager = mockSourceManager(),
            entryRepository = mockk(relaxed = true) {
                coEvery { getEntryById(any()) } answers {
                    entries.firstOrNull { it.id == firstArg<Long>() }
                }
            },
            mediaSession = MangaMediaSessionProcessor(noOpMediaSession()),
            entryInteractionPreferences = entryInteractionPreferences,
        )
    }

    private fun mangaConsumptionProcessor(
        repository: EntryChapterRepository,
        progressRepository: EntryProgressRepository = FakeEntryProgressRepository(emptyList()),
    ): MangaConsumptionProcessor {
        return MangaConsumptionProcessor(
            entryChapterRepository = repository,
            entryProgressRepository = progressRepository,
        )
    }

    private fun noOpMediaSession() = EntryMediaSessionEventSink {
        EntryMediaSessionResult.Handled
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
            every { this@mockk.isDownloaderRunning } returns MutableStateFlow(false)
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

    private fun entry(
        type: EntryType,
        id: Long = 1L,
        title: String = "Entry",
        sourceId: Long = 1L,
        profileId: Long = 1L,
    ): Entry {
        return Entry.create().copy(id = id, title = title, source = sourceId, profileId = profileId, type = type)
    }

    private fun chapter(
        id: Long = 1L,
        entryId: Long = 1L,
        url: String = "/chapter/$id",
        name: String = "Chapter",
        read: Boolean = false,
        bookmark: Boolean = false,
        sourceOrder: Long = 0L,
        dateUpload: Long = 0L,
        chapterNumber: Double = 0.0,
    ): EntryChapter {
        return EntryChapter.create().copy(
            id = id,
            entryId = entryId,
            url = url,
            name = name,
            read = read,
            bookmark = bookmark,
            sourceOrder = sourceOrder,
            dateUpload = dateUpload,
            chapterNumber = chapterNumber,
        )
    }

    private fun pageProgress(
        entryId: Long = 1L,
        chapterId: Long,
        pageIndex: Long,
        completed: Boolean = false,
        updatedAt: Long = 1L,
    ): EntryProgressState {
        return mangaProgressState(
            entryId = entryId,
            chapterId = chapterId,
            resourceKey = "/chapter/$chapterId",
            pageIndex = pageIndex,
            pageCount = 10L,
            completed = completed,
            locatorUpdatedAt = updatedAt,
            completionUpdatedAt = updatedAt,
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

    private class FakeEntryProgressRepository(
        initialStates: List<EntryProgressState>,
    ) : EntryProgressRepository {
        private val states = initialStates.toMutableList()
        val upsertedStates = mutableListOf<EntryProgressState>()

        override suspend fun get(entryId: Long, contentKey: String, resourceKey: String): EntryProgressState? {
            return states.firstOrNull {
                it.entryId == entryId && it.contentKey == contentKey && it.resourceKey == resourceKey
            }
        }

        override suspend fun getByEntryId(entryId: Long): List<EntryProgressState> =
            states.filter { it.entryId == entryId }

        override fun getByEntryIdAsFlow(entryId: Long): Flow<List<EntryProgressState>> =
            flowOf(states.filter { it.entryId == entryId })

        override fun getByChapterIdAsFlow(chapterId: Long): Flow<List<EntryProgressState>> =
            flowOf(states.filter { it.chapterId == chapterId })

        override suspend fun upsert(state: EntryProgressState) = record(state)

        override suspend fun upsertAndSyncChild(state: EntryProgressState) = record(state)

        override suspend fun merge(state: EntryProgressState): EntryProgressState = state.also(::record)

        override suspend fun mergeAndSyncChild(state: EntryProgressState): EntryProgressState = state.also(::record)

        override suspend fun rekey(
            entryId: Long,
            chapterId: Long?,
            oldContentKey: String,
            oldResourceKey: String,
            newContentKey: String,
            newResourceKey: String,
        ) = Unit

        private fun record(state: EntryProgressState) {
            states.removeAll { it.identity == state.identity }
            states += state
            upsertedStates += state
        }
    }
}
