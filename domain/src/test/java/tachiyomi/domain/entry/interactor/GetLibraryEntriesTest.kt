package tachiyomi.domain.entry.interactor

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.MergedEntryRepository
import tachiyomi.domain.entry.service.EntryLibraryProgressCalculator
import tachiyomi.domain.entry.service.EntryLibraryProgressResolver
import tachiyomi.domain.entry.service.EntryLibraryState
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.ProgressState
import tachiyomi.domain.source.model.SourceDisplayInfo
import tachiyomi.domain.source.service.HiddenSourceIds
import tachiyomi.domain.source.service.SourceManager

class GetLibraryEntriesTest {

    private val entryRepository = mockk<EntryRepository>()
    private val entryChapterRepository = mockk<EntryChapterRepository>()
    private val categoryRepository = mockk<CategoryRepository>()
    private val mergedEntryRepository = mockk<MergedEntryRepository>()
    private val hiddenSourceIds = mockk<HiddenSourceIds>()
    private val sourceManager = mockk<SourceManager>()
    private val entryLibraryProgressResolver = EntryLibraryProgressResolver(
        listOf(
            testCalculator(EntryType.MANGA),
            testCalculator(EntryType.ANIME),
        ),
    )

    private val interactor = GetLibraryEntries(
        entryRepository = entryRepository,
        entryChapterRepository = entryChapterRepository,
        entryLibraryProgressResolver = entryLibraryProgressResolver,
        categoryRepository = categoryRepository,
        mergedEntryRepository = mergedEntryRepository,
        hiddenSourceIds = hiddenSourceIds,
        sourceManager = sourceManager,
    )

    @Test
    fun `filters manga and anime entries using the same hidden source set`() = runTest {
        val manga = entry(id = 1L, source = 10L, type = EntryType.MANGA)
        val anime = entry(id = 2L, source = 20L, type = EntryType.ANIME)

        coEvery { entryRepository.getLibraryEntries() } returns listOf(manga, anime)
        coEvery { entryRepository.getLibraryLastRead() } returns emptyMap()
        coEvery { mergedEntryRepository.getAll() } returns emptyList()
        every { hiddenSourceIds.get() } returns setOf(10L, 20L)
        every { entryChapterRepository.getChaptersByEntryIds(listOf(1L, 2L)) } returns flowOf(emptyList())
        coEvery { categoryRepository.getCategoryIdsByEntryIds(listOf(1L, 2L)) } returns mapOf(
            1L to listOf(1L),
            2L to listOf(1L),
        )
        every { sourceManager.getOrStub(10L) } returns source(10L)
        every { sourceManager.getOrStub(20L) } returns source(20L)
        every { sourceManager.getDisplayInfo(10L) } returns sourceDisplayInfo(10L)
        every { sourceManager.getDisplayInfo(20L) } returns sourceDisplayInfo(20L)

        interactor.await().map { it.entry.id } shouldBe emptyList()
    }

    @Test
    fun `uses default category for manga and anime entries without category mappings`() = runTest {
        val manga = entry(id = 1L, source = 10L, type = EntryType.MANGA)
        val anime = entry(id = 2L, source = 20L, type = EntryType.ANIME)

        coEvery { entryRepository.getLibraryEntries() } returns listOf(manga, anime)
        coEvery { entryRepository.getLibraryLastRead() } returns mapOf(1L to 100L, 2L to 200L)
        coEvery { mergedEntryRepository.getAll() } returns emptyList()
        every { hiddenSourceIds.get() } returns emptySet()
        every { entryChapterRepository.getChaptersByEntryIds(listOf(1L, 2L)) } returns flowOf(emptyList())
        coEvery { categoryRepository.getCategoryIdsByEntryIds(listOf(1L, 2L)) } returns emptyMap()
        every { sourceManager.getOrStub(10L) } returns source(10L)
        every { sourceManager.getOrStub(20L) } returns source(20L)
        every { sourceManager.getDisplayInfo(10L) } returns sourceDisplayInfo(10L)
        every { sourceManager.getDisplayInfo(20L) } returns sourceDisplayInfo(20L)

        val items = interactor.await()
        items.map { it.categories } shouldBe listOf(
            listOf(Category.UNCATEGORIZED_ID),
            listOf(Category.UNCATEGORIZED_ID),
        )
        items.map { it.lastRead } shouldBe listOf(100L, 200L)
    }

    private fun entry(id: Long, source: Long, type: EntryType): Entry {
        return Entry.create().copy(
            id = id,
            source = source,
            favorite = true,
            initialized = true,
            title = "Entry $id",
            type = type,
        )
    }

    private fun source(id: Long): UnifiedSource {
        val source = mockk<UnifiedSource>()
        every { source.id } returns id
        every { source.name } returns "Source $id"
        return source
    }

    private fun sourceDisplayInfo(id: Long): SourceDisplayInfo {
        return SourceDisplayInfo(
            id = id,
            name = "Source $id",
            lang = "",
            isMissing = false,
        )
    }

    private fun testCalculator(type: EntryType): EntryLibraryProgressCalculator {
        return object : EntryLibraryProgressCalculator {
            override val entryType = type

            override suspend fun calculate(
                entry: Entry,
                chapters: List<EntryChapter>,
                lastRead: Long,
            ): EntryLibraryState {
                return EntryLibraryState(progress(type, chapters.size.toLong()), lastRead, continueEntryId = null)
            }

            override fun merge(members: List<LibraryItem>): EntryLibraryState {
                return EntryLibraryState(
                    progress(
                        type,
                        members.sumOf {
                            it.totalCount
                        },
                    ),
                    lastRead = 0L,
                    continueEntryId = null,
                )
            }
        }
    }

    private fun progress(type: EntryType, totalCount: Long): ProgressState {
        return when (type) {
            EntryType.MANGA -> ProgressState(
                totalCount = totalCount,
                consumedCount = 0L,
                hasStarted = false,
            )
            EntryType.ANIME -> ProgressState(
                totalCount = totalCount,
                consumedCount = 0L,
                hasStarted = false,
                continueMode = ProgressState.ContinueMode.TARGET_AVAILABLE,
            )
        }
    }
}
