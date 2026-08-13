package eu.kanade.tachiyomi.ui.library

import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryItemKey

internal data class LibraryCategorySelectionPreparation(
    val common: Collection<Category>,
    val mixed: Collection<Category>,
)

internal suspend fun prepareLibraryCategorySelection(
    items: List<LibraryItem>,
    getCategoriesForItem: suspend (LibraryItem) -> List<Category>,
): LibraryCategorySelectionPreparation {
    if (items.isEmpty()) return LibraryCategorySelectionPreparation(emptyList(), emptyList())

    val categoriesByItem = items.map { getCategoriesForItem(it).toSet() }
    val common = categoriesByItem.reduce { first, second -> first.intersect(second) }
    val mixed = categoriesByItem.flatten().distinct().subtract(common)
    return LibraryCategorySelectionPreparation(common, mixed)
}

internal suspend fun categoriesForLibraryItem(
    item: LibraryItem,
    getCategories: suspend (Long) -> List<Category>,
): List<Category> {
    return item.memberEntryIds
        .map(LibraryItemKey::id)
        .distinct()
        .flatMap { getCategories(it) }
        .distinctBy(Category::id)
}
