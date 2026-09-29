package eu.kanade.tachiyomi.ui.library

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

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
}
