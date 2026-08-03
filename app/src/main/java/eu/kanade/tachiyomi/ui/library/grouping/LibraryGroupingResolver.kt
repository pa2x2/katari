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
        LibraryGroupingDimension.Category -> visibleCategories.mapNotNull { category ->
            val groupItems = items.filter { category.id in it.categories }
            if (!preserveEmptyCategories && groupItems.isEmpty()) return@mapNotNull null
            ResolvedLibraryGroup(
                tab = LibraryPageTab(
                    id = "category:${category.id}",
                    title = category.name,
                    category = category,
                    dimension = dimension,
                ),
                category = category,
                items = groupItems,
            )
        }
        LibraryGroupingDimension.EntryType -> EntryType.entries.mapNotNull { entryType ->
            val groupItems = items.filter { it.entry.type == entryType }
            if (groupItems.isEmpty()) return@mapNotNull null
            ResolvedLibraryGroup(
                tab = LibraryPageTab(
                    id = "type:${entryType.name}",
                    title = entryTypeTitle(entryType),
                    dimension = dimension,
                ),
                entryType = entryType,
                items = groupItems,
            )
        }
        LibraryGroupingDimension.Source -> {
            val sourceNames = items.associate { it.displaySourceId to it.sourceName }
            sourceNames.keys
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
                            dimension = dimension,
                        ),
                        sourceId = sourceId,
                        items = items.filter { it.displaySourceId == sourceId },
                    )
                }
        }
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
