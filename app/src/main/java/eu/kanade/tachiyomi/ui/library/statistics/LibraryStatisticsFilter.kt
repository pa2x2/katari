package eu.kanade.tachiyomi.ui.library.statistics

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.library.model.LibraryItem

enum class LibraryStatisticsFilterKind {
    LIBRARY,
    OFFLINE,
    TRACKED,
}

data class LibraryStatisticsFilter(
    val kind: LibraryStatisticsFilterKind,
    val typeName: String?,
) {
    val type: EntryType? = typeName?.let { name -> EntryType.entries.firstOrNull { it.name == name } }
}

internal fun filterLibraryStatisticsItems(
    items: List<LibraryItem>,
    filter: LibraryStatisticsFilter,
    downloadCount: (LibraryItem) -> Int,
    trackedEntryIds: Set<Long>,
): List<LibraryItem> {
    if (filter.typeName != null && filter.type == null) return emptyList()
    return items
        .asSequence()
        .filter { item -> filter.type == null || item.entry.type == filter.type }
        .filter { item ->
            when (filter.kind) {
                LibraryStatisticsFilterKind.LIBRARY -> true
                LibraryStatisticsFilterKind.OFFLINE -> downloadCount(item) > 0
                LibraryStatisticsFilterKind.TRACKED -> item.memberEntries.any { it.id in trackedEntryIds }
            }
        }
        .sortedBy { it.title.lowercase() }
        .toList()
}
