package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.service.EntryLibraryProgressResolution
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryItemKey
import tachiyomi.domain.library.model.LibraryItemSource

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
    return LibraryItem(
        entry = entry,
        categories = listOf(0L),
        sources = orderedSourceIds.map { LibraryItemSource(it, "Source $it", "en") },
        sourceItemOrientation = EntryItemOrientation.VERTICAL,
        isLocal = false,
        isMerged = memberIds.size > 1,
        memberEntryIds = memberEntries.map { LibraryItemKey(EntryType.ANIME, it.id) },
        memberEntries = memberEntries,
        progressSummary = EntryLibraryProgressResolution.Inapplicable(EntryType.ANIME),
        latestUpload = 0L,
        downloadCount = 0,
    )
}
