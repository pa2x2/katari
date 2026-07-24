package tachiyomi.domain.entry.interactor

import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.service.EntryLibraryContinueTarget
import tachiyomi.domain.entry.service.EntryLibraryGroupResolution
import tachiyomi.domain.entry.service.EntryLibraryGroupingResolution
import tachiyomi.domain.entry.service.EntryLibraryGroupingResolutionPort
import tachiyomi.domain.entry.service.EntryLibraryProgressResolution
import tachiyomi.domain.entry.service.EntryLibraryProgressResolutionPort
import tachiyomi.domain.entry.service.EntryLibraryProgressSummary
import tachiyomi.domain.source.model.EntrySourceDescription
import tachiyomi.domain.source.model.SourceDisplayInfo
import tachiyomi.domain.source.service.EntrySourceDescriptionResolutionPort
import tachiyomi.domain.source.service.HiddenSourceIds
import tachiyomi.domain.source.service.SourceManager

class GetLibraryEntriesTest {

    private val entryRepository = mockk<EntryRepository>()
    private val entryChapterRepository = mockk<EntryChapterRepository>()
    private val categoryRepository = mockk<CategoryRepository>()
    private val libraryGrouping = mockk<EntryLibraryGroupingResolutionPort>()
    private val hiddenSourceIds = mockk<HiddenSourceIds>()
    private val sourceManager = mockk<SourceManager>()
    private val sourceDescription = EntrySourceDescriptionResolutionPort {
        EntrySourceDescription(
            language = "",
            supportedEntryTypes = null,
            itemOrientation = EntryItemOrientation.VERTICAL,
            catalogue = null,
        )
    }
    private val unavailableSummaryEntryIds = mutableSetOf<Long>()
    private val entryLibraryProgressResolver = testProgressPort()

    private val interactor = GetLibraryEntries(
        entryRepository = entryRepository,
        entryChapterRepository = entryChapterRepository,
        entryLibraryProgressResolver = entryLibraryProgressResolver,
        categoryRepository = categoryRepository,
        libraryGrouping = libraryGrouping,
        hiddenSourceIds = hiddenSourceIds,
        sourceManager = sourceManager,
        sourceDescription = sourceDescription,
    )

    @Test
    fun `filters manga and anime entries using the same hidden source set`() = runTest {
        val manga = entry(id = 1L, source = 10L, type = EntryType.MANGA)
        val anime = entry(id = 2L, source = 20L, type = EntryType.ANIME)

        coEvery { entryRepository.getLibraryEntries() } returns listOf(manga, anime)
        coEvery { entryRepository.getLibraryLastRead(manga.profileId) } returns emptyMap()
        stubStandaloneGrouping(listOf(manga, anime))
        every { hiddenSourceIds.get(manga.profileId) } returns setOf(10L, 20L)
        every { entryChapterRepository.getChaptersByEntryIds(listOf(1L, 2L)) } returns flowOf(emptyList())
        coEvery { categoryRepository.getCategoryIdsByEntryIds(manga.profileId, listOf(1L, 2L)) } returns mapOf(
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
        coEvery {
            entryRepository.getLibraryLastRead(manga.profileId)
        } returns mapOf(1L to 100L, 2L to 200L)
        stubStandaloneGrouping(listOf(manga, anime))
        every { hiddenSourceIds.get(manga.profileId) } returns emptySet()
        every { entryChapterRepository.getChaptersByEntryIds(listOf(1L, 2L)) } returns flowOf(emptyList())
        coEvery {
            categoryRepository.getCategoryIdsByEntryIds(manga.profileId, listOf(1L, 2L))
        } returns emptyMap()
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

    @Test
    fun `entry without progress summary remains structurally visible`() = runTest {
        val book = entry(id = 3L, source = 30L, type = EntryType.BOOK)
        unavailableSummaryEntryIds += book.id
        coEvery { entryRepository.getLibraryEntries() } returns listOf(book)
        coEvery { entryRepository.getLibraryLastRead(book.profileId) } returns emptyMap()
        stubStandaloneGrouping(listOf(book))
        every { hiddenSourceIds.get(book.profileId) } returns emptySet()
        every { entryChapterRepository.getChaptersByEntryIds(listOf(book.id)) } returns flowOf(emptyList())
        coEvery {
            categoryRepository.getCategoryIdsByEntryIds(book.profileId, listOf(book.id))
        } returns emptyMap()
        every { sourceManager.getOrStub(book.source) } returns source(book.source)
        every { sourceManager.getDisplayInfo(book.source) } returns sourceDisplayInfo(book.source)

        val item = interactor.await().single()

        item.entry shouldBe book
        item.progressSummary shouldBe EntryLibraryProgressResolution.Inapplicable(EntryType.BOOK)
        item.totalCount shouldBe null
    }

    @Test
    fun `library grouping collapses supplied members in feature order`() = runTest {
        val target = entry(id = 1L, source = 10L, type = EntryType.MANGA)
        val member = entry(id = 2L, source = 20L, type = EntryType.MANGA)
        val favorites = listOf(target, member)
        coEvery { entryRepository.getLibraryEntries() } returns favorites
        coEvery { entryRepository.getLibraryLastRead(target.profileId) } returns emptyMap()
        coEvery { libraryGrouping.resolveLibraryGrouping(target.profileId, favorites) } returns
            EntryLibraryGroupingResolution(
                profileId = target.profileId,
                groups = listOf(EntryLibraryGroupResolution(target, listOf(target, member))),
            )
        every { hiddenSourceIds.get(target.profileId) } returns emptySet()
        every { entryChapterRepository.getChaptersByEntryIds(listOf(1L, 2L)) } returns flowOf(emptyList())
        coEvery { categoryRepository.getCategoryIdsByEntryIds(target.profileId, listOf(1L, 2L)) } returns mapOf(
            target.id to listOf(10L),
            member.id to listOf(20L),
        )
        every { sourceManager.getOrStub(target.source) } returns source(target.source)
        every { sourceManager.getOrStub(member.source) } returns source(member.source)
        every { sourceManager.getDisplayInfo(target.source) } returns sourceDisplayInfo(target.source)
        every { sourceManager.getDisplayInfo(member.source) } returns sourceDisplayInfo(member.source)

        val item = interactor.await().single()

        item.entry shouldBe target
        item.isMerged shouldBe true
        item.memberEntries shouldBe listOf(target, member)
        item.categories shouldBe listOf(10L, 20L)
    }

    @Test
    fun `subscription resolves all profile owned data for requested profile`() = runTest {
        val profileId = 2L
        val entry = entry(id = 1L, source = 10L, type = EntryType.MANGA, profileId = profileId)
        every { entryRepository.getLibraryEntriesAsFlow(profileId) } returns flowOf(listOf(entry))
        every {
            libraryGrouping.observeLibraryGrouping(profileId, any())
        } returns flowOf(
            EntryLibraryGroupingResolution(
                profileId = profileId,
                groups = listOf(EntryLibraryGroupResolution(entry, listOf(entry))),
            ),
        )
        every { hiddenSourceIds.subscribe(profileId) } returns flowOf(emptySet())
        every { entryChapterRepository.getChaptersByEntryIds(listOf(entry.id)) } returns flowOf(emptyList())
        coEvery {
            categoryRepository.getCategoryIdsByEntryIds(profileId, listOf(entry.id))
        } returns emptyMap()
        coEvery { entryRepository.getLibraryLastRead(profileId) } returns emptyMap()
        every { sourceManager.getOrStub(entry.source) } returns source(entry.source)
        every { sourceManager.getDisplayInfo(entry.source) } returns sourceDisplayInfo(entry.source)

        interactor.subscribe(profileId).first().single().entry shouldBe entry
    }

    private fun entry(
        id: Long,
        source: Long,
        type: EntryType,
        profileId: Long = 0L,
    ): Entry {
        return Entry.create().copy(
            id = id,
            source = source,
            favorite = true,
            initialized = true,
            title = "Entry $id",
            type = type,
            profileId = profileId,
        )
    }

    private fun stubStandaloneGrouping(entries: List<Entry>) {
        coEvery { libraryGrouping.resolveLibraryGrouping(any(), entries) } returns EntryLibraryGroupingResolution(
            profileId = entries.firstOrNull()?.profileId ?: 0L,
            groups = entries.map { entry -> EntryLibraryGroupResolution(entry, listOf(entry)) },
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

    private fun testProgressPort(): EntryLibraryProgressResolutionPort {
        return object : EntryLibraryProgressResolutionPort {
            override suspend fun calculate(
                entry: Entry,
                chapters: List<EntryChapter>,
                lastRead: Long,
            ): EntryLibraryProgressResolution {
                if (entry.id in unavailableSummaryEntryIds) {
                    return EntryLibraryProgressResolution.Inapplicable(entry.type)
                }
                return EntryLibraryProgressResolution.Available(summary(chapters.size.toLong(), lastRead))
            }

            override fun merge(
                entryType: EntryType,
                members: List<EntryLibraryProgressSummary>,
            ): EntryLibraryProgressResolution {
                return EntryLibraryProgressResolution.Available(
                    summary(members.sumOf(EntryLibraryProgressSummary::totalCount), lastRead = 0L),
                )
            }
        }
    }

    private fun summary(totalCount: Long, lastRead: Long): EntryLibraryProgressSummary {
        return EntryLibraryProgressSummary(
            totalCount = totalCount,
            consumedCount = 0L,
            hasStarted = false,
            bookmarkCount = null,
            inProgressItemId = null,
            inProgressFraction = null,
            lastRead = lastRead,
            continueTarget = EntryLibraryContinueTarget.Inapplicable,
        )
    }
}
