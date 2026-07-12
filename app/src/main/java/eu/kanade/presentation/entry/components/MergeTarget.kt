package eu.kanade.presentation.entry.components

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.source.service.SourceManager

@Immutable
data class MergeTarget(
    val id: Long,
    val searchableTitle: String,
    val isMerged: Boolean,
    val memberEntries: ImmutableList<Entry>,
    val categoryIds: List<Long>,
    val entry: MergeEditorEntry,
) : MergeSearchTarget {
    override val mergeSearchTitle: String
        get() = entry.title

    override val mergeSearchableTitle: String
        get() = searchableTitle
}

internal fun buildMergeTargets(
    libraryItems: List<LibraryItem>,
    sourceManager: SourceManager,
    entryType: EntryType? = null,
    excludedEntryIds: Set<Long> = emptySet(),
): ImmutableList<MergeTarget> {
    return libraryItems.mapNotNull { item ->
        if (entryType != null && item.entry.type != entryType) {
            return@mapNotNull null
        }
        if (entryType != null && item.memberEntries.any { it.type != entryType }) {
            return@mapNotNull null
        }
        if (item.memberEntryIds.any { it.id in excludedEntryIds }) {
            return@mapNotNull null
        }
        MergeTarget(
            id = item.entry.id,
            searchableTitle = item.memberEntries.flatMap { memberEntry ->
                listOfNotNull(memberEntry.title, memberEntry.displayName)
            }
                .distinct()
                .joinToString(" "),
            isMerged = item.isMerged,
            memberEntries = item.memberEntries.toImmutableList(),
            categoryIds = item.categories,
            entry = MergeEditorEntry(
                id = item.entry.id,
                entry = item.entry,
                subtitle = buildString {
                    append(sourceManager.getDisplayInfo(item.displaySourceId).name)
                    if (item.isMerged) {
                        append(" • ")
                        append(item.memberEntries.size)
                        append(" ")
                        append(if (item.memberEntries.size == 1) "member" else "members")
                    }
                },
            ),
        )
    }.toImmutableList()
}
