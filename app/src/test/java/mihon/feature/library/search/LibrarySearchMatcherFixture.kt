package mihon.feature.library.search

import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.service.EntryLibraryContinueTarget
import tachiyomi.domain.entry.service.EntryLibraryProgressResolution
import tachiyomi.domain.entry.service.EntryLibraryProgressSummary
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryItemKey
import tachiyomi.source.local.LocalSource

internal fun librarySearchMatcher(
    query: String,
    categoryNamesById: Map<Long, String> = emptyMap(),
    sourceNames: (LibraryItem) -> List<String> = { listOf(it.sourceName) },
) = LibrarySearchMatcher(query, categoryNamesById, sourceNames = sourceNames)

internal fun searchEntry(
    id: Long = 1L,
    source: Long = 10L,
    type: EntryType = EntryType.MANGA,
    displayName: String? = null,
    description: String? = null,
    notes: String = "",
) = Entry.create().copy(
    id = id,
    source = source,
    type = type,
    profileId = 42L,
    title = "Original title",
    displayName = displayName,
    description = description,
    notes = notes,
)

internal fun searchLibraryItem(
    entry: Entry,
    categories: List<Long> = emptyList(),
    sourceIds: Set<Long> = setOf(entry.source),
    displaySourceId: Long = entry.source,
    sourceLanguage: String = "en",
    progressSummary: EntryLibraryProgressResolution = EntryLibraryProgressResolution.Inapplicable(entry.type),
): LibraryItem {
    val memberEntries = sourceIds.mapIndexed { index, sourceId ->
        entry.copy(id = entry.id + index, source = sourceId)
    }
    return LibraryItem(
        entry = entry,
        categories = categories,
        sourceName = if (sourceIds.size > 1) "Multiple sources" else "Test source",
        sourceLanguage = sourceLanguage,
        sourceItemOrientation = EntryItemOrientation.VERTICAL,
        displaySourceId = displaySourceId,
        sourceIds = sourceIds,
        isLocal = displaySourceId == LocalSource.ID,
        isMerged = memberEntries.size > 1,
        memberEntryIds = memberEntries.map { LibraryItemKey(it.type, it.id) },
        memberEntries = memberEntries,
        progressSummary = progressSummary,
        latestUpload = 0L,
        downloadCount = 0,
    )
}

internal fun availableSearchProgress(total: Long, consumed: Long) = EntryLibraryProgressResolution.Available(
    EntryLibraryProgressSummary(
        totalCount = total,
        consumedCount = consumed,
        hasStarted = consumed > 0L,
        bookmarkCount = 0L,
        inProgressItemId = null,
        inProgressFraction = null,
        lastRead = 0L,
        continueTarget = EntryLibraryContinueTarget.NoNext,
    ),
)
