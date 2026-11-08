package eu.kanade.presentation.library.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.selection.selectableItem
import eu.kanade.tachiyomi.ui.library.LibraryPage
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryItemKey

/**
 * What the items of library pages do when tapped or long-pressed, and under a mouse: Ctrl+click [onToggle]s an item,
 * Shift+click selects the range to it, and the item's menu opens it. Selection is per page, so [on] binds them to the
 * page an item is shown on.
 */
@Immutable
class LibraryPageClicks(
    val onClick: (LibraryPage, LibraryItem) -> Unit,
    val onLongClick: (LibraryPage, LibraryItem) -> Unit,
    val onToggle: (LibraryPage, LibraryItem) -> Unit,
    val onSelectRange: (LibraryPage, LibraryItem) -> Unit,
    val onOpen: (LibraryItem) -> Unit,
) {
    fun on(page: LibraryPage) = LibraryItemClicks(
        onClick = { onClick(page, it) },
        onLongClick = { onLongClick(page, it) },
        onToggle = { onToggle(page, it) },
        onSelectRange = { onSelectRange(page, it) },
        onOpen = onOpen,
    )
}

/** [LibraryPageClicks] bound to one page. */
@Immutable
class LibraryItemClicks(
    val onClick: (LibraryItem) -> Unit,
    val onLongClick: (LibraryItem) -> Unit,
    val onToggle: (LibraryItem) -> Unit,
    val onSelectRange: (LibraryItem) -> Unit,
    val onOpen: (LibraryItem) -> Unit,
)

/** Makes [item]'s cover or row answer the mouse as [clicks] say. */
@Composable
internal fun Modifier.libraryItemPointer(
    item: LibraryItem,
    selection: Set<LibraryItemKey>,
    clicks: LibraryItemClicks,
): Modifier = selectableItem(
    selected = item.key in selection,
    onToggle = { clicks.onToggle(item) },
    onSelectRange = { clicks.onSelectRange(item) },
    onOpen = { clicks.onOpen(item) },
)
