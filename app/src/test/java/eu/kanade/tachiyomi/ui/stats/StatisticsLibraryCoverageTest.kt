package eu.kanade.tachiyomi.ui.stats

import eu.kanade.presentation.more.stats.data.StatsOfflineCoverage
import eu.kanade.presentation.more.stats.data.StatsTrackingCoverage
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.service.EntryLibraryContinueTarget
import tachiyomi.domain.entry.service.EntryLibraryProgressResolution
import tachiyomi.domain.entry.service.EntryLibraryProgressSummary
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryItemKey

class StatisticsLibraryCoverageTest {

    @Test
    fun `coverage preserves logical tracking and never calls a merged work fully offline`() {
        val complete = item(id = 1L, type = EntryType.MANGA, total = 10L)
        val mergedMember = Entry.create().copy(id = 3L, type = EntryType.MANGA)
        val merged = item(
            id = 2L,
            type = EntryType.MANGA,
            total = 10L,
            isMerged = true,
            extraMembers = listOf(mergedMember),
        )
        val anime = item(id = 4L, type = EntryType.ANIME, total = 12L)
        val counts = mapOf(1L to 10, 2L to 6, 4L to 0)

        val result = buildLibraryCoverage(
            items = listOf(complete, merged, anime),
            types = listOf(EntryType.MANGA, EntryType.ANIME, EntryType.BOOK),
            downloadApplicable = { it != EntryType.BOOK },
            downloadCount = { counts[it.entry.id] ?: 0 },
            trackingApplicable = { it != EntryType.BOOK },
            connectedTrackingTypes = setOf(EntryType.MANGA),
            trackedEntryIds = setOf(mergedMember.id),
        )

        result.getValue(EntryType.MANGA).offline shouldBe StatsOfflineCoverage(
            partlyOfflineTitles = 1,
            fullyOfflineTitles = 1,
        )
        result.getValue(EntryType.MANGA).tracking shouldBe StatsTrackingCoverage.Connected(
            trackedTitles = 1,
            totalTitles = 2,
        )
        result.getValue(EntryType.ANIME).tracking shouldBe StatsTrackingCoverage.NotConnected
        result.getValue(EntryType.BOOK).offline shouldBe null
        result.getValue(EntryType.BOOK).tracking shouldBe StatsTrackingCoverage.Unsupported
    }

    private fun item(
        id: Long,
        type: EntryType,
        total: Long,
        isMerged: Boolean = false,
        extraMembers: List<Entry> = emptyList(),
    ): LibraryItem {
        val entry = Entry.create().copy(id = id, type = type)
        val members = listOf(entry) + extraMembers
        return LibraryItem(
            entry = entry,
            categories = emptyList(),
            sourceName = "Source",
            sourceLanguage = "en",
            sourceItemOrientation = EntryItemOrientation.VERTICAL,
            displaySourceId = entry.source,
            sourceIds = setOf(entry.source),
            isLocal = false,
            isMerged = isMerged,
            memberEntryIds = members.map { LibraryItemKey(type, it.id) },
            memberEntries = members,
            progressSummary = EntryLibraryProgressResolution.Available(
                EntryLibraryProgressSummary(
                    totalCount = total,
                    consumedCount = 0L,
                    hasStarted = false,
                    bookmarkCount = 0L,
                    inProgressItemId = null,
                    inProgressFraction = null,
                    lastRead = 0L,
                    continueTarget = EntryLibraryContinueTarget.NoNext,
                ),
            ),
            latestUpload = 0L,
            downloadCount = 0,
        )
    }
}
