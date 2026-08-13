package eu.kanade.tachiyomi.ui.library

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.category.model.Category

class LibraryCategorySelectionPreparationTest {

    @Test
    fun `selection resolves common and mixed categories with one load per item`() = runTest {
        val first = libraryItem(id = 1L)
        val second = libraryItem(id = 2L)
        val categories = mapOf(
            first.entry.id to listOf(category(10L), category(20L)),
            second.entry.id to listOf(category(20L), category(30L)),
        )
        val loads = mutableListOf<Long>()

        val selection = prepareLibraryCategorySelection(listOf(first, second)) { item ->
            loads += item.entry.id
            categories.getValue(item.entry.id)
        }

        selection.common.map(Category::id).toSet() shouldBe setOf(20L)
        selection.mixed.map(Category::id).toSet() shouldBe setOf(10L, 30L)
        loads shouldBe listOf(first.entry.id, second.entry.id)
    }

    @Test
    fun `merged item categories include every distinct member category once`() = runTest {
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
    fun `empty selection avoids category loading`() = runTest {
        var loadCount = 0

        val selection = prepareLibraryCategorySelection(emptyList()) {
            loadCount += 1
            emptyList()
        }

        selection shouldBe LibraryCategorySelectionPreparation(emptyList(), emptyList())
        loadCount shouldBe 0
    }

    @Test
    fun `single item categories are all common and none mixed`() = runTest {
        val categories = listOf(category(10L), category(20L))

        val selection = prepareLibraryCategorySelection(listOf(libraryItem(1L))) { categories }

        selection.common shouldBe categories.toSet()
        selection.mixed shouldBe emptySet()
    }

    @Test
    fun `category loading failure propagates without loading the suffix`() = runTest {
        val items = listOf(libraryItem(1L), libraryItem(2L), libraryItem(3L))
        val loads = mutableListOf<Long>()
        val failure = IllegalStateException("category read failed")

        shouldThrow<IllegalStateException> {
            prepareLibraryCategorySelection(items) { item ->
                loads += item.entry.id
                if (item.entry.id == 2L) throw failure
                emptyList()
            }
        } shouldBe failure

        loads shouldBe listOf(1L, 2L)
    }
}
