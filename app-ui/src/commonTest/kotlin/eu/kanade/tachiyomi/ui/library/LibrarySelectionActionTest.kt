package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.service.EntryLibraryProgressResolution
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryItemKey
import tachiyomi.domain.library.model.LibraryItemSource

class LibrarySelectionActionTest {

    @Test
    fun `category actions update every distinct merged member`() = runTest {
        val currentCategories = mapOf(
            1L to emptyList(),
            2L to emptyList(),
            3L to listOf(20L),
        )
        val updates = mutableMapOf<Long, List<Long>>()

        updateLibraryItemCategories(
            items = listOf(
                libraryItem(id = 1L, memberIds = listOf(1L, 2L)),
                libraryItem(id = 2L),
                libraryItem(id = 3L),
            ),
            addCategories = listOf(10L),
            removeCategories = listOf(20L),
            getCategoryIds = { currentCategories[it].orEmpty() },
            setCategoryIds = { entryId, categoryIds -> updates[entryId] = categoryIds },
        )

        updates shouldBe mapOf(
            1L to listOf(10L),
            2L to listOf(10L),
            3L to listOf(10L),
        )
    }

    private fun libraryItem(id: Long, memberIds: List<Long> = listOf(id)): LibraryItem {
        val memberEntries = memberIds.map { memberId ->
            Entry.create().copy(
                id = memberId,
                source = memberId,
                favorite = true,
                title = "Entry $memberId",
                type = EntryType.ANIME,
            )
        }
        return LibraryItem(
            entry = memberEntries.first { it.id == id },
            categories = listOf(0L),
            sources = memberEntries.map { LibraryItemSource(it.source, "Source ${it.source}", "en") },
            sourceItemOrientation = EntryItemOrientation.VERTICAL,
            isLocal = false,
            isMerged = memberIds.size > 1,
            memberEntryIds = memberEntries.map { LibraryItemKey(EntryType.ANIME, it.id) },
            memberEntries = memberEntries,
            progressSummary = EntryLibraryProgressResolution.Inapplicable(EntryType.ANIME),
            latestUpload = 0L,
            downloadCount = 0,
        )
    }
}
