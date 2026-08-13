package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.service.EntryLibraryProgressResolution
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryItemKey

internal fun libraryItem(
    id: Long,
    memberIds: List<Long> = listOf(id),
    sourceIds: Set<Long> = memberIds.mapTo(linkedSetOf()) { it },
): LibraryItem {
    val orderedSourceIds = sourceIds.toList()
    val memberEntries = memberIds.distinct().mapIndexed { index, memberId ->
        Entry.create().copy(
            id = memberId,
            source = orderedSourceIds.getOrElse(index) { orderedSourceIds.first() },
            favorite = true,
            title = "Entry $memberId",
            type = EntryType.ANIME,
        )
    }
    val entry = memberEntries.first { it.id == id }
    val displaySourceId = sourceIds.singleOrNull() ?: LibraryItem.MULTI_SOURCE_ID
    val isMultiSource = displaySourceId == LibraryItem.MULTI_SOURCE_ID
    return LibraryItem(
        entry = entry,
        categories = listOf(0L),
        sourceName = if (isMultiSource) "" else "Source",
        sourceLanguage = if (isMultiSource) LibraryItem.MULTI_SOURCE_ID.toString() else "en",
        sourceItemOrientation = EntryItemOrientation.VERTICAL,
        displaySourceId = displaySourceId,
        sourceIds = sourceIds,
        isLocal = false,
        isMerged = memberIds.size > 1,
        memberEntryIds = memberEntries.map { LibraryItemKey(EntryType.ANIME, it.id) },
        memberEntries = memberEntries,
        progressSummary = EntryLibraryProgressResolution.Inapplicable(EntryType.ANIME),
        latestUpload = 0L,
        downloadCount = 0,
    )
}

internal fun category(id: Long): Category {
    return Category(id = id, name = "Category $id", order = id, flags = 0L)
}
