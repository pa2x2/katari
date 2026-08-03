package eu.kanade.tachiyomi.ui.library

import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryGroupType
import tachiyomi.domain.library.model.LibraryItem

/** Compatibility boundary for the legacy two-level grouping behavior covered by existing callers. */
internal fun buildTypeLibraryPages(
    items: List<LibraryItem>,
    visibleCategories: List<Category>,
    groupType: LibraryGroupType,
    categoryTabs: Map<Long, LibraryPageTab>,
    entryTypes: List<EntryType>,
    typeTabs: Map<EntryType, LibraryPageTab>,
): List<LibraryPage> {
    return when (groupType) {
        LibraryGroupType.Type -> {
            entryTypes.map { entryType ->
                LibraryPage(
                    id = "type:${entryType.name}",
                    primaryTab = typeTabs.getValue(entryType),
                    entryType = entryType,
                    itemIds = items.fastFilter { it.entry.type == entryType }
                        .fastMap(LibraryItem::key),
                )
            }
        }
        LibraryGroupType.TypeCategory -> {
            buildList {
                entryTypes.forEach { entryType ->
                    val typeItems = items.fastFilter { it.entry.type == entryType }
                    visibleCategories.forEach { category ->
                        val itemIds = typeItems.fastFilter { category.id in it.categories }
                            .fastMap(LibraryItem::key)
                        if (itemIds.isNotEmpty()) {
                            add(
                                LibraryPage(
                                    id = "type:${entryType.name}:category:${category.id}",
                                    primaryTab = typeTabs.getValue(entryType),
                                    secondaryTab = categoryTabs.getValue(category.id),
                                    category = category,
                                    entryType = entryType,
                                    itemIds = itemIds,
                                ),
                            )
                        }
                    }
                }
            }
        }
        LibraryGroupType.CategoryType -> {
            buildList {
                visibleCategories.forEach { category ->
                    val categoryItems = items.fastFilter { category.id in it.categories }
                    if (categoryItems.isEmpty()) {
                        add(
                            LibraryPage(
                                id = "category:${category.id}",
                                primaryTab = categoryTabs.getValue(category.id),
                                category = category,
                            ),
                        )
                    } else {
                        entryTypes.forEach { entryType ->
                            val itemIds = categoryItems.fastFilter { it.entry.type == entryType }
                                .fastMap(LibraryItem::key)
                            if (itemIds.isNotEmpty()) {
                                add(
                                    LibraryPage(
                                        id = "category:${category.id}:type:${entryType.name}",
                                        primaryTab = categoryTabs.getValue(category.id),
                                        secondaryTab = typeTabs.getValue(entryType),
                                        category = category,
                                        entryType = entryType,
                                        itemIds = itemIds,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
        else -> error("Unsupported type grouping mode: $groupType")
    }
}
