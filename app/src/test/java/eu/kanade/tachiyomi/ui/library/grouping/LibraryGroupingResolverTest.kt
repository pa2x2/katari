package eu.kanade.tachiyomi.ui.library.grouping

import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.service.EntryLibraryProgressResolution
import tachiyomi.domain.library.model.LibraryGrouping
import tachiyomi.domain.library.model.LibraryGroupingDimension
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryItemKey

class LibraryGroupingResolverTest {

    @Test
    fun `ungrouped library retains the all page and item order`() {
        val items = listOf(item(id = 2L), item(id = 1L))

        val pages = resolve(items, dimensions = emptyList())

        pages.map(LibraryPageSnapshot::from) shouldContainExactly listOf(
            LibraryPageSnapshot(
                id = "all",
                primaryTitle = "Library",
                itemIds = listOf(2L, 1L),
            ),
        )
    }

    @Test
    fun `category grouping retains category order overlap and top level empty pages`() {
        val system = category(id = 0L, name = "System")
        val empty = category(id = 2L, name = "Empty")
        val reading = category(id = 1L, name = "Reading")
        val items = listOf(
            item(id = 1L, categories = listOf(1L, 1L, 0L)),
            item(id = 2L, categories = listOf(1L)),
        )

        val hiddenSystemPages = resolve(
            items = items,
            categories = listOf(system, empty, reading),
            dimensions = listOf(LibraryGroupingDimension.Category),
        )
        val visibleSystemPages = resolve(
            items = items,
            categories = listOf(system, empty, reading),
            dimensions = listOf(LibraryGroupingDimension.Category),
            showSystemCategory = true,
        )

        hiddenSystemPages.map(LibraryPageSnapshot::from) shouldContainExactly listOf(
            LibraryPageSnapshot("category:2", "Empty", emptyList()),
            LibraryPageSnapshot("category:1", "Reading", listOf(1L, 2L)),
        )
        visibleSystemPages.map(LibraryPageSnapshot::from) shouldContainExactly listOf(
            LibraryPageSnapshot("category:0", "System", listOf(1L)),
            LibraryPageSnapshot("category:2", "Empty", emptyList()),
            LibraryPageSnapshot("category:1", "Reading", listOf(1L, 2L)),
        )
    }

    @Test
    fun `three dimensions retain configured hierarchy and path metadata`() {
        val first = category(id = 1L, name = "First")
        val second = category(id = 2L, name = "Second")
        val items = listOf(
            item(id = 1L, type = EntryType.MANGA, categories = listOf(1L), sourceId = 20L, sourceName = "Beta"),
            item(id = 2L, type = EntryType.ANIME, categories = listOf(1L), sourceId = 10L, sourceName = "Alpha"),
            item(id = 3L, type = EntryType.MANGA, categories = listOf(1L, 2L), sourceId = 10L, sourceName = "Alpha"),
            item(id = 4L, type = EntryType.BOOK, categories = listOf(2L), sourceId = 30L, sourceName = "Gamma"),
        )

        val pages = resolve(
            items = items,
            categories = listOf(first, second),
            dimensions = listOf(
                LibraryGroupingDimension.Category,
                LibraryGroupingDimension.Source,
                LibraryGroupingDimension.EntryType,
            ),
        )

        pages.map { it.id to it.itemIds.map(LibraryItemKey::id) } shouldContainExactly listOf(
            "category:1/source:10/type:MANGA" to listOf(3L),
            "category:1/source:10/type:ANIME" to listOf(2L),
            "category:1/source:20/type:MANGA" to listOf(1L),
            "category:2/source:10/type:MANGA" to listOf(3L),
            "category:2/source:30/type:BOOK" to listOf(4L),
        )
        pages.first().let { page ->
            page.category shouldBe first
            page.sourceId shouldBe 10L
            page.entryType shouldBe EntryType.MANGA
            page.primaryTab.dimension shouldBe LibraryGroupingDimension.Category
            page.secondaryTab?.dimension shouldBe LibraryGroupingDimension.Source
            page.tertiaryTab?.dimension shouldBe LibraryGroupingDimension.EntryType
        }
    }

    @Test
    fun `source grouping orders equal names by source id and retains item encounter order`() {
        val items = listOf(
            item(id = 1L, sourceId = 20L, sourceName = "Alpha"),
            item(id = 2L, sourceId = 10L, sourceName = "Alpha"),
            item(id = 3L, sourceId = 20L, sourceName = "Alpha"),
            item(id = 4L, sourceId = 30L, sourceName = "Beta"),
        )

        val pages = resolve(
            items = items,
            dimensions = listOf(LibraryGroupingDimension.Source),
        )

        pages.map(LibraryPageSnapshot::from) shouldContainExactly listOf(
            LibraryPageSnapshot("source:10", "Alpha", listOf(2L)),
            LibraryPageSnapshot("source:20", "Alpha", listOf(1L, 3L)),
            LibraryPageSnapshot("source:30", "Beta", listOf(4L)),
        )
    }

    private fun resolve(
        items: List<LibraryItem>,
        categories: List<Category> = emptyList(),
        dimensions: List<LibraryGroupingDimension>,
        showSystemCategory: Boolean = false,
    ) = resolveLibraryPages(
        items = items,
        categories = categories,
        showSystemCategory = showSystemCategory,
        grouping = LibraryGrouping(dimensions),
        libraryTitle = "Library",
        entryTypeTitle = EntryType::name,
    )
}

private data class LibraryPageSnapshot(
    val id: String,
    val primaryTitle: String,
    val itemIds: List<Long>,
) {
    companion object {
        fun from(page: eu.kanade.tachiyomi.ui.library.LibraryPage) = LibraryPageSnapshot(
            id = page.id,
            primaryTitle = page.primaryTab.title,
            itemIds = page.itemIds.map(LibraryItemKey::id),
        )
    }
}

private fun category(id: Long, name: String) = Category(
    id = id,
    name = name,
    order = id,
    flags = 0L,
)

private fun item(
    id: Long,
    type: EntryType = EntryType.MANGA,
    categories: List<Long> = emptyList(),
    sourceId: Long = id,
    sourceName: String = "Source $sourceId",
): LibraryItem {
    val entry = Entry.create().copy(
        id = id,
        source = sourceId,
        favorite = true,
        title = "Entry $id",
        type = type,
    )
    return LibraryItem(
        entry = entry,
        categories = categories,
        sourceName = sourceName,
        sourceLanguage = "en",
        sourceItemOrientation = EntryItemOrientation.VERTICAL,
        displaySourceId = sourceId,
        sourceIds = setOf(sourceId),
        isLocal = false,
        isMerged = false,
        memberEntryIds = listOf(LibraryItemKey(type, id)),
        memberEntries = listOf(entry),
        progressSummary = EntryLibraryProgressResolution.Inapplicable(type),
        latestUpload = 0L,
        downloadCount = 0,
    )
}
