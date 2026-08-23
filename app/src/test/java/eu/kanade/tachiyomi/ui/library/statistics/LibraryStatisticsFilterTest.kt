package eu.kanade.tachiyomi.ui.library.statistics

import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.service.EntryLibraryContinueTarget
import tachiyomi.domain.entry.service.EntryLibraryProgressResolution
import tachiyomi.domain.entry.service.EntryLibraryProgressSummary
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryItemKey

class LibraryStatisticsFilterTest {

    @Test
    fun `typed filters preserve merged work membership`() {
        val manga = item(id = 1L, type = EntryType.MANGA, memberIds = listOf(10L))
        val anime = item(id = 2L, type = EntryType.ANIME, memberIds = listOf(20L))

        val offline = filterLibraryStatisticsItems(
            items = listOf(anime, manga),
            filter = LibraryStatisticsFilter(LibraryStatisticsFilterKind.OFFLINE, EntryType.MANGA.name),
            downloadCount = { if (it.entry.id == manga.entry.id) 2 else 0 },
            trackedEntryIds = emptySet(),
        )
        val tracked = filterLibraryStatisticsItems(
            items = listOf(anime, manga),
            filter = LibraryStatisticsFilter(LibraryStatisticsFilterKind.TRACKED, EntryType.MANGA.name),
            downloadCount = { 0 },
            trackedEntryIds = setOf(10L),
        )

        offline.map { it.entry.id } shouldContainExactly listOf(manga.entry.id)
        tracked.map { it.entry.id } shouldContainExactly listOf(manga.entry.id)
    }

    private fun item(id: Long, type: EntryType, memberIds: List<Long>): LibraryItem {
        val entry = Entry.create().copy(id = id, type = type, title = "Title $id")
        val members = listOf(entry) + memberIds.map { Entry.create().copy(id = it, type = type) }
        return LibraryItem(
            entry = entry,
            categories = emptyList(),
            sourceName = "Source",
            sourceLanguage = "en",
            sourceItemOrientation = EntryItemOrientation.VERTICAL,
            displaySourceId = entry.source,
            sourceIds = setOf(entry.source),
            isLocal = false,
            isMerged = members.size > 1,
            memberEntryIds = members.map { LibraryItemKey(type, it.id) },
            memberEntries = members,
            progressSummary = EntryLibraryProgressResolution.Available(
                EntryLibraryProgressSummary(
                    totalCount = 1L,
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
