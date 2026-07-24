package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.service.EntryLibraryProgressResolution
import tachiyomi.domain.library.model.LibraryGroupType
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryItemKey

class LibraryTypeGroupingTest {

    private val categories = listOf(
        Category(id = 1L, name = "First", order = 0L, flags = 0L),
        Category(id = 2L, name = "Empty", order = 1L, flags = 0L),
    )
    private val categoryTabs = categories.associate { category ->
        category.id to LibraryPageTab("category:${category.id}", category.name, category)
    }
    private val entryTypes = EntryType.entries
    private val typeTabs = entryTypes.associateWith { type ->
        LibraryPageTab("type:${type.name}", type.name)
    }
    private val items = listOf(
        libraryItem(id = 1L, type = EntryType.MANGA, categories = listOf(1L)),
        libraryItem(id = 2L, type = EntryType.ANIME, categories = listOf(1L)),
    )

    @Test
    fun `type grouping creates stable pages in entry type order`() {
        val pages = pagesFor(LibraryGroupType.Type)

        pages.map { it.id } shouldContainExactly listOf("type:MANGA", "type:ANIME", "type:BOOK")
        pages.map { it.entryType } shouldContainExactly listOf(
            EntryType.MANGA,
            EntryType.ANIME,
            EntryType.BOOK,
        )
        pages.flatMap { it.itemIds }.toSet() shouldBe items.map { it.key }.toSet()
    }

    @Test
    fun `type category grouping emits only non-empty intersections`() {
        val pages = pagesFor(LibraryGroupType.TypeCategory)

        pages.map { it.id } shouldContainExactly listOf(
            "type:MANGA:category:1",
            "type:ANIME:category:1",
        )
        pages.all { it.itemIds.size == 1 } shouldBe true
    }

    @Test
    fun `category type grouping preserves an empty category page`() {
        val pages = pagesFor(LibraryGroupType.CategoryType)

        pages.map { it.id } shouldContainExactly listOf(
            "category:1:type:MANGA",
            "category:1:type:ANIME",
            "category:2",
        )
        pages.last().itemIds shouldBe emptyList()
    }

    private fun pagesFor(groupType: LibraryGroupType): List<LibraryPage> {
        return buildTypeLibraryPages(
            items = items,
            visibleCategories = categories,
            groupType = groupType,
            categoryTabs = categoryTabs,
            entryTypes = entryTypes,
            typeTabs = typeTabs,
        )
    }
}

private fun libraryItem(id: Long, type: EntryType, categories: List<Long>): LibraryItem {
    val entry = Entry.create().copy(
        id = id,
        source = id,
        favorite = true,
        title = "Entry $id",
        type = type,
    )
    return LibraryItem(
        entry = entry,
        categories = categories,
        sourceName = "Source",
        sourceLanguage = "en",
        sourceItemOrientation = EntryItemOrientation.VERTICAL,
        displaySourceId = entry.source,
        sourceIds = setOf(entry.source),
        isLocal = false,
        isMerged = false,
        memberEntryIds = listOf(LibraryItemKey(type, id)),
        memberEntries = listOf(entry),
        progressSummary = EntryLibraryProgressResolution.Inapplicable(type),
        latestUpload = 0L,
        downloadCount = 0,
    )
}
