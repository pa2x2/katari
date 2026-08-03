package eu.kanade.tachiyomi.ui.library

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryGroupingDimension
import tachiyomi.domain.library.model.LibraryItemKey

@Immutable
data class LibraryPage(
    val id: String,
    val primaryTab: LibraryPageTab,
    val secondaryTab: LibraryPageTab? = null,
    val tertiaryTab: LibraryPageTab? = null,
    val category: Category? = null,
    val sourceId: Long? = null,
    val entryType: EntryType? = null,
    val itemIds: List<LibraryItemKey> = emptyList(),
)

@Immutable
data class LibraryPageTab(
    val id: String,
    val title: String,
    val category: Category? = null,
    val dimension: LibraryGroupingDimension? = null,
)

fun LibraryPage.displayTitle(defaultCategoryTitle: String): String {
    val primaryTitle = primaryTab.displayTitle(defaultCategoryTitle)
    val secondaryTitle = secondaryTab?.displayTitle(defaultCategoryTitle)
    val tertiaryTitle = tertiaryTab?.displayTitle(defaultCategoryTitle)
    return listOfNotNull(primaryTitle, secondaryTitle, tertiaryTitle).joinToString(" / ")
}

fun LibraryPageTab.displayTitle(defaultCategoryTitle: String): String {
    return if (category?.isSystemCategory == true) {
        defaultCategoryTitle
    } else {
        title
    }
}
