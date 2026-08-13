package eu.kanade.tachiyomi.ui.library.grouping

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.library.LibraryPage
import eu.kanade.tachiyomi.ui.library.LibraryPageTab
import tachiyomi.core.common.util.lang.compareToWithCollator
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryGrouping
import tachiyomi.domain.library.model.LibraryGroupingDimension
import tachiyomi.domain.library.model.LibraryItem

internal fun resolveLibraryPages(
    items: List<LibraryItem>,
    categories: List<Category>,
    showSystemCategory: Boolean,
    grouping: LibraryGrouping,
    libraryTitle: String,
    entryTypeTitle: (EntryType) -> String,
): List<LibraryPage> {
    val visibleCategories = categories.filter { showSystemCategory || !it.isSystemCategory }
    if (grouping.dimensions.isEmpty()) {
        return listOf(
            LibraryPage(
                id = ALL_ENTRIES_ID,
                primaryTab = LibraryPageTab(id = ALL_ENTRIES_ID, title = libraryTitle),
                itemIds = items.map(LibraryItem::key),
            ),
        )
    }

    return buildList {
        resolveLevel(
            items = items,
            dimensions = grouping.dimensions,
            depth = 0,
            path = emptyList(),
            visibleCategories = visibleCategories,
            entryTypeTitle = entryTypeTitle,
            destination = this,
        )
    }
}

private fun resolveLevel(
    items: List<LibraryItem>,
    dimensions: List<LibraryGroupingDimension>,
    depth: Int,
    path: List<ResolvedLibraryGroup>,
    visibleCategories: List<Category>,
    entryTypeTitle: (EntryType) -> String,
    destination: MutableList<LibraryPage>,
) {
    if (depth == dimensions.size || items.isEmpty()) {
        if (path.isNotEmpty()) destination += path.toPage(items)
        return
    }

    val groups = resolveGroups(
        items = items,
        dimension = dimensions[depth],
        preserveEmptyCategories = depth == 0,
        visibleCategories = visibleCategories,
        entryTypeTitle = entryTypeTitle,
    )
    if (groups.isEmpty()) {
        if (path.isNotEmpty()) destination += path.toPage(items)
        return
    }

    groups.forEach { group ->
        resolveLevel(
            items = group.items,
            dimensions = dimensions,
            depth = depth + 1,
            path = path + group,
            visibleCategories = visibleCategories,
            entryTypeTitle = entryTypeTitle,
            destination = destination,
        )
    }
}

private fun resolveGroups(
    items: List<LibraryItem>,
    dimension: LibraryGroupingDimension,
    preserveEmptyCategories: Boolean,
    visibleCategories: List<Category>,
    entryTypeTitle: (EntryType) -> String,
): List<ResolvedLibraryGroup> {
    return when (dimension) {
        LibraryGroupingDimension.Category -> resolveCategoryGroups(
            items = items,
            preserveEmptyCategories = preserveEmptyCategories,
            visibleCategories = visibleCategories,
        )
        LibraryGroupingDimension.EntryType -> resolveEntryTypeGroups(items, entryTypeTitle)
        LibraryGroupingDimension.Source -> resolveSourceGroups(items)
    }
}

private fun resolveCategoryGroups(
    items: List<LibraryItem>,
    preserveEmptyCategories: Boolean,
    visibleCategories: List<Category>,
): List<ResolvedLibraryGroup> {
    val bucketPositionsByCategoryId = buildMap<Long, MutableList<Int>> {
        visibleCategories.forEachIndexed { index, category ->
            getOrPut(category.id, ::mutableListOf) += index
        }
    }
    val buckets = List(visibleCategories.size) { mutableListOf<LibraryItem>() }
    val lastItemIndexByBucket = IntArray(visibleCategories.size) { -1 }

    items.forEachIndexed { itemIndex, item ->
        item.categories.forEach { categoryId ->
            bucketPositionsByCategoryId[categoryId]?.forEach { bucketIndex ->
                if (lastItemIndexByBucket[bucketIndex] != itemIndex) {
                    buckets[bucketIndex] += item
                    lastItemIndexByBucket[bucketIndex] = itemIndex
                }
            }
        }
    }

    return visibleCategories.mapIndexedNotNull { index, category ->
        val groupItems = buckets[index]
        if (!preserveEmptyCategories && groupItems.isEmpty()) return@mapIndexedNotNull null
        ResolvedLibraryGroup(
            tab = LibraryPageTab(
                id = "category:${category.id}",
                title = category.name,
                category = category,
                dimension = LibraryGroupingDimension.Category,
            ),
            category = category,
            items = groupItems,
        )
    }
}

private fun resolveEntryTypeGroups(
    items: List<LibraryItem>,
    entryTypeTitle: (EntryType) -> String,
): List<ResolvedLibraryGroup> {
    val buckets = List(EntryType.entries.size) { mutableListOf<LibraryItem>() }
    items.forEach { item -> buckets[item.entry.type.ordinal] += item }

    return EntryType.entries.mapIndexedNotNull { index, entryType ->
        val groupItems = buckets[index]
        if (groupItems.isEmpty()) return@mapIndexedNotNull null
        ResolvedLibraryGroup(
            tab = LibraryPageTab(
                id = "type:${entryType.name}",
                title = entryTypeTitle(entryType),
                dimension = LibraryGroupingDimension.EntryType,
            ),
            entryType = entryType,
            items = groupItems,
        )
    }
}

private fun resolveSourceGroups(items: List<LibraryItem>): List<ResolvedLibraryGroup> {
    val sourceNames = mutableMapOf<Long, String>()
    val buckets = mutableMapOf<Long, MutableList<LibraryItem>>()
    items.forEach { item ->
        sourceNames[item.displaySourceId] = item.sourceName
        buckets.getOrPut(item.displaySourceId, ::mutableListOf) += item
    }

    return sourceNames.keys
        .sortedWith { first, second ->
            sourceNames.getValue(first)
                .compareToWithCollator(sourceNames.getValue(second))
                .takeIf { it != 0 }
                ?: first.compareTo(second)
        }
        .map { sourceId ->
            ResolvedLibraryGroup(
                tab = LibraryPageTab(
                    id = "source:$sourceId",
                    title = sourceNames.getValue(sourceId),
                    dimension = LibraryGroupingDimension.Source,
                ),
                sourceId = sourceId,
                items = buckets.getValue(sourceId),
            )
        }
}

private fun List<ResolvedLibraryGroup>.toPage(items: List<LibraryItem>): LibraryPage {
    val tabs = map(ResolvedLibraryGroup::tab)
    return LibraryPage(
        id = joinToString(separator = "/") { it.tab.id },
        primaryTab = tabs[0],
        secondaryTab = tabs.getOrNull(1),
        tertiaryTab = tabs.getOrNull(2),
        category = firstNotNullOfOrNull(ResolvedLibraryGroup::category),
        sourceId = firstNotNullOfOrNull(ResolvedLibraryGroup::sourceId),
        entryType = firstNotNullOfOrNull(ResolvedLibraryGroup::entryType),
        itemIds = items.map(LibraryItem::key),
    )
}

private data class ResolvedLibraryGroup(
    val tab: LibraryPageTab,
    val category: Category? = null,
    val sourceId: Long? = null,
    val entryType: EntryType? = null,
    val items: List<LibraryItem>,
)

private const val ALL_ENTRIES_ID = "all"
