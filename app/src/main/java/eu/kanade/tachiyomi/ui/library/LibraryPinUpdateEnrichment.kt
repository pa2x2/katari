package eu.kanade.tachiyomi.ui.library

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.library.model.LibraryItem

/**
 * Keeps source and download enrichment attached to library items when the only
 * database change is their pinned state.
 */
internal fun Flow<List<LibraryItem>>.reuseEnrichmentForPinUpdates(
    enrich: (List<LibraryItem>) -> List<LibraryItem>,
): Flow<List<LibraryItem>> = flow {
    var previousSourceItems: List<LibraryItem>? = null
    var previousEnrichedItems: List<LibraryItem>? = null

    collect { sourceItems ->
        val enrichedItems = previousSourceItems
            ?.takeUnless { sourceItems === it }
            ?.let { previous ->
                previousEnrichedItems?.applyPinUpdate(
                    previousSourceItems = previous,
                    currentSourceItems = sourceItems,
                )
            }
            ?: enrich(sourceItems)

        previousSourceItems = sourceItems
        previousEnrichedItems = enrichedItems
        emit(enrichedItems)
    }
}

private fun List<LibraryItem>.applyPinUpdate(
    previousSourceItems: List<LibraryItem>,
    currentSourceItems: List<LibraryItem>,
): List<LibraryItem>? {
    if (size != previousSourceItems.size || size != currentSourceItems.size) return null

    var pinChanged = false
    for (index in indices) {
        val enrichedItem = this[index]
        val previousItem = previousSourceItems[index]
        val currentItem = currentSourceItems[index]
        if (enrichedItem.key != currentItem.key || previousItem.key != currentItem.key) return null
        if (currentItem.copy(entry = previousItem.entry, memberEntries = previousItem.memberEntries) != previousItem) {
            return null
        }
        if (!currentItem.entry.hasOnlyPinMutationFrom(previousItem.entry)) return null
        pinChanged = pinChanged || currentItem.entry.libraryPinned != previousItem.entry.libraryPinned
        if (currentItem.memberEntries.size != previousItem.memberEntries.size) return null
        for (memberIndex in currentItem.memberEntries.indices) {
            val currentMember = currentItem.memberEntries[memberIndex]
            val previousMember = previousItem.memberEntries[memberIndex]
            if (!currentMember.hasOnlyPinMutationFrom(previousMember)) return null
            pinChanged = pinChanged || currentMember.libraryPinned != previousMember.libraryPinned
        }
    }
    if (!pinChanged) return null

    return mapIndexed { index, enrichedItem ->
        val currentItem = currentSourceItems[index]
        enrichedItem.copy(
            entry = currentItem.entry,
            memberEntries = currentItem.memberEntries,
        )
    }
}

private fun Entry.hasOnlyPinMutationFrom(previous: Entry): Boolean {
    return copy(
        libraryPinned = previous.libraryPinned,
        lastModifiedAt = previous.lastModifiedAt,
        version = previous.version,
    ) == previous
}
