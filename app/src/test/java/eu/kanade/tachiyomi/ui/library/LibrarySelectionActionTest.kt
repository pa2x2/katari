package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.service.EntryLibraryProgressResolution
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryItemKey

class LibrarySelectionActionTest {

    @Test
    fun `selection actions preserve distinct merged member ids`() {
        selectedActionEntryIds(
            listOf(
                libraryItem(id = 1L, memberIds = listOf(1L, 2L)),
                libraryItem(id = 2L),
                libraryItem(id = 3L),
            ),
        ) shouldBe listOf(1L, 2L, 3L)
    }

    @Test
    fun `merged item categories include every member category`() = runTest {
        val categories = mapOf(
            1L to listOf(category(10L)),
            2L to listOf(category(20L), category(10L)),
        )

        categoriesForLibraryItem(
            item = libraryItem(id = 1L, memberIds = listOf(1L, 2L)),
            getCategories = { categories[it].orEmpty() },
        ).map(Category::id) shouldBe listOf(10L, 20L)
    }

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

    @Test
    fun `merged download execution keeps the source set used by availability`() {
        val member = Entry.create().copy(id = 2L, source = 20L, type = EntryType.ANIME)
        val selected = listOf(
            libraryItem(
                id = 1L,
                memberIds = listOf(1L, member.id),
                sourceIds = setOf(10L, member.source),
            ),
        )

        selected.downloadSourceIdsFor(member) shouldBe setOf(10L, 20L)
    }
}

private fun libraryItem(
    id: Long,
    memberIds: List<Long> = listOf(id),
    sourceIds: Set<Long> = setOf(1L),
): LibraryItem {
    val entry = Entry.create().copy(
        id = id,
        source = 1L,
        favorite = true,
        title = "Entry $id",
        type = EntryType.ANIME,
    )
    return LibraryItem(
        entry = entry,
        categories = listOf(0L),
        sourceName = "Source",
        sourceLanguage = "en",
        sourceItemOrientation = EntryItemOrientation.VERTICAL,
        displaySourceId = 1L,
        sourceIds = sourceIds,
        isLocal = false,
        isMerged = memberIds.size > 1,
        memberEntryIds = memberIds.map { LibraryItemKey(EntryType.ANIME, it) },
        memberEntries = listOf(entry),
        progressSummary = EntryLibraryProgressResolution.Inapplicable(EntryType.ANIME),
        latestUpload = 0L,
        downloadCount = 0,
    )
}

private fun category(id: Long): Category {
    return Category(id = id, name = "Category $id", order = id, flags = 0L)
}
