package mihon.entry.interactions.manga.runtime

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.manga.navigation.MangaContinueProcessor
import mihon.entry.interactions.manga.navigation.MangaOpenProcessor
import mihon.entry.interactions.manga.state.MangaConsumptionProcessor
import mihon.entry.interactions.manga.state.MangaProgressProcessor
import mihon.entry.interactions.manga.state.lastReadAt
import mihon.entry.interactions.manga.state.mangaProgressState
import mihon.entry.interactions.manga.state.pageIndex
import mihon.entry.interactions.state.EntryProgressResourceMapping
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository

class MangaEntryInteractionPluginTest {

    @Test
    fun `manga progress copy maps page state to target resource`() = runTest {
        val progressRepository = FakeEntryProgressRepository(
            listOf(pageProgress(entryId = 1L, chapterId = 10L, pageIndex = 4L)),
        )
        val processor = MangaProgressProcessor(progressRepository, FakeEntryChapterRepository(emptyList()))

        processor.copy(
            sourceEntry = manga(id = 1L),
            targetEntry = manga(id = 2L),
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
    fun `manga continue starts at first unread chapter in reading order`() = runTest {
        val first = chapter(id = 1L, sourceOrder = 1L, chapterNumber = 1.0)
        val latest = chapter(id = 2L, sourceOrder = 0L, chapterNumber = 2.0)
        val processor = continueProcessor(chapters = listOf(latest, first))

        processor.findNext(manga()) shouldBe first
    }

    @Test
    fun `manga continue prefers most recently updated partial chapter from merged member`() = runTest {
        val rootChapter = chapter(id = 1L, entryId = 1L, read = false)
        val siblingChapter = chapter(id = 3L, entryId = 2L, read = false)
        val processor = continueProcessor(
            chapters = listOf(rootChapter, siblingChapter),
            progressStates = listOf(
                pageProgress(entryId = 1L, chapterId = 1L, pageIndex = 2L, updatedAt = 10L),
                pageProgress(entryId = 2L, chapterId = 3L, pageIndex = 4L, updatedAt = 20L),
            ),
        )

        processor.findNext(manga()) shouldBe siblingChapter
    }

    @Test
    fun `manga consumption marks only unread chapters read without changing recency`() = runTest {
        val unread = chapter(id = 1L, read = false)
        val progressRepository = FakeEntryProgressRepository(emptyList())

        val changed = MangaConsumptionProcessor(FakeEntryChapterRepository(emptyList()), progressRepository)
            .setConsumed(
                entry = manga(),
                chapters = listOf(unread, chapter(id = 2L, read = true)),
                consumed = true,
            )

        changed.shouldContainExactly(unread)
        progressRepository.upsertedStates.map { Triple(it.chapterId, it.completed, it.lastReadAt) }
            .shouldContainExactly(Triple(1L, true, 0L))
    }

    @Test
    fun `manga consumption marks unread and resets progress without changing recency`() = runTest {
        val progressRepository = FakeEntryProgressRepository(
            listOf(
                pageProgress(chapterId = 1L, pageIndex = 5L, completed = true),
                pageProgress(chapterId = 2L, pageIndex = 4L),
            ),
        )

        MangaConsumptionProcessor(FakeEntryChapterRepository(emptyList()), progressRepository).setConsumed(
            entry = manga(),
            chapters = listOf(
                chapter(id = 1L, read = true),
                chapter(id = 2L, read = false),
            ),
            consumed = false,
        )

        progressRepository.upsertedStates.map {
            listOf(it.chapterId, it.pageIndex, it.completed, it.locatorUpdatedAt, it.completionUpdatedAt)
        }.shouldContainExactly(
            listOf(1L, 0L, false, 1L, 1L),
            listOf(2L, 0L, false, 1L, 1L),
        )
    }

    private fun continueProcessor(
        chapters: List<EntryChapter>,
        progressStates: List<EntryProgressState> = emptyList(),
    ): MangaContinueProcessor {
        val getEntryWithChapters = mockk<GetEntryWithChapters> {
            coEvery { awaitChapters(any()) } returns chapters.sortedBy { it.sourceOrder }
        }
        return MangaContinueProcessor(
            getEntryWithChapters = getEntryWithChapters,
            entryProgressRepository = FakeEntryProgressRepository(progressStates),
            openProcessor = MangaOpenProcessor(),
        )
    }

    private fun manga(id: Long = 1L): Entry =
        Entry.create().copy(id = id, title = "Entry", source = 1L, profileId = 1L, type = EntryType.MANGA)

    private fun chapter(
        id: Long = 1L,
        entryId: Long = 1L,
        read: Boolean = false,
        sourceOrder: Long = 0L,
        chapterNumber: Double = 0.0,
    ): EntryChapter {
        return EntryChapter.create().copy(
            id = id,
            entryId = entryId,
            url = "/chapter/$id",
            name = "Chapter",
            read = read,
            sourceOrder = sourceOrder,
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

        override suspend fun updateAll(chapters: List<EntryChapter>): Boolean = true

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
