package eu.kanade.tachiyomi.ui.library

import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryItemKey

internal fun List<LibraryItemKey>.prioritizePinned(
    favoritesById: Map<LibraryItemKey, LibraryItem>,
): List<LibraryItemKey> {
    val (pinned, unpinned) = partition { itemId -> favoritesById[itemId]?.isPinned == true }
    return pinned + unpinned
}
