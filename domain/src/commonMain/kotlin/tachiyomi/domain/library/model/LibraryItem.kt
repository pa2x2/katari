package tachiyomi.domain.library.model

import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.service.EntryLibraryContinueTarget
import tachiyomi.domain.entry.service.EntryLibraryProgressResolution
import tachiyomi.domain.entry.service.EntryLibraryProgressSummary

/**
 * Unified library item. A single data class wraps an [Entry] plus progress and
 * sort metadata.
 */
data class LibraryItem(
    val entry: Entry,
    val categories: List<Long>,
    /** Distinct sources of the members, in member order; one entry for an unmerged item. */
    val sources: List<LibraryItemSource>,
    val sourceItemOrientation: EntryItemOrientation,
    val isLocal: Boolean,
    val isMerged: Boolean,
    val memberEntryIds: List<LibraryItemKey>,
    val memberEntries: List<Entry>,
    val progressSummary: EntryLibraryProgressResolution,
    val latestUpload: Long,
    val downloadCount: Int,
) {
    val key: LibraryItemKey
        get() = LibraryItemKey(entry.type, entry.id)

    val sourceId: Long
        get() = entry.source

    val sourceIds: Set<Long>
        get() = sources.mapTo(mutableSetOf(), LibraryItemSource::id)

    val isMultiSource: Boolean
        get() = sources.size > 1

    /** Distinct non-blank source languages, in member order. */
    val languages: List<String>
        get() = sources.map(LibraryItemSource::language).filter(String::isNotBlank).distinct()

    val title: String
        get() = entry.displayTitle

    val cover: String?
        get() = entry.thumbnailUrl

    val favorite: Boolean
        get() = entry.favorite

    val isPinned: Boolean
        get() = memberEntries.any(Entry::libraryPinned)

    val dateAdded: Long
        get() = entry.dateAdded

    val favoriteModifiedAt: Long
        get() = entry.favoriteModifiedAt ?: entry.dateAdded

    val availableProgressSummary: EntryLibraryProgressSummary?
        get() = (progressSummary as? EntryLibraryProgressResolution.Available)?.summary

    val hasProgressSummary: Boolean
        get() = availableProgressSummary != null

    val totalCount: Long?
        get() = availableProgressSummary?.totalCount

    val consumedCount: Long?
        get() = availableProgressSummary?.consumedCount

    val unconsumedCount: Long?
        get() = availableProgressSummary?.unconsumedCount

    val hasStarted: Boolean?
        get() = availableProgressSummary?.hasStarted

    val hasBookmarks: Boolean?
        get() = availableProgressSummary?.bookmarkCount?.let { it > 0 }

    val hasInProgress: Boolean
        get() = availableProgressSummary?.inProgressItemId != null

    val progressFraction: Float?
        get() = availableProgressSummary?.inProgressFraction

    val lastRead: Long?
        get() = availableProgressSummary?.lastRead

    val canContinue: Boolean
        get() = availableProgressSummary?.continueTarget is EntryLibraryContinueTarget.Available
}

data class LibraryItemSource(
    val id: Long,
    val name: String,
    val language: String,
)
