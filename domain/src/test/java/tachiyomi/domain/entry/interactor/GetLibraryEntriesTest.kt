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
