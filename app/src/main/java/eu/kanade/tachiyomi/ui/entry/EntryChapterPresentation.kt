package eu.kanade.tachiyomi.ui.entry

import eu.kanade.tachiyomi.source.entry.ChapterKind
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import mihon.entry.interactions.child.EntryChildListFeature
import mihon.entry.interactions.child.EntryChildListRequest
import mihon.entry.interactions.child.EntryChildListResult
import mihon.entry.interactions.child.EntryChildListRow
import mihon.entry.interactions.child.EntryScanlatorFilterFeature
import mihon.entry.interactions.child.EntryScanlatorFilterResult
import mihon.entry.interactions.child.EntryScanlatorFilterStateResult
import tachiyomi.domain.entry.model.ChapterGroupId
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryChapterGroup
import tachiyomi.domain.entry.model.groupId
import tachiyomi.domain.entry.service.EntryChapterFilters

data class EntryChapterPresentation(
    val processedChapters: PersistentList<EntryChapterList.Item>,
    val rows: PersistentList<EntryChapterList>,
    val selectedChapters: PersistentList<EntryChapterList.Item>,
    val aggregateMissingCount: Int,
    val hasUnreadChapters: Boolean,
    val hasReadChapters: Boolean,
    /** Chapters left after excluded scanlators but before the read, download and bookmark filters. */
    val progress: EntryChapterProgress,
    /** Whether any chapter, filtered out or not, is filler, which offers its filter. */
    val hasFiller: Boolean,
    /** Whether any chapter has a still or summary, which offers the switch that hides them. */
    val hasPreviews: Boolean,
    private val rawIndexByChapterId: Map<Long, Int>,
    private val processedIndexByChapterId: Map<Long, Int>,
    private val rowIndexByChapterId: Map<Long, Int>,
    private val selectedChapterIds: PersistentSet<Long>,
    val sourceIds: PersistentSet<Long>,
) {
    val isAnySelected: Boolean
        get() = selectedChapterIds.isNotEmpty()

    fun rawIndexOf(chapterId: Long): Int? = rawIndexByChapterId[chapterId]

    fun processedIndexOf(chapterId: Long): Int? = processedIndexByChapterId[chapterId]

    fun rowIndexOf(chapterId: Long): Int? = rowIndexByChapterId[chapterId]

    fun updateItems(items: Collection<EntryChapterList.Item>): EntryChapterPresentation {
        var updatedProcessedChapters = processedChapters
        var updatedRows = rows
        var updatedSelectedChapters = selectedChapters
        var updatedSelectedChapterIds = selectedChapterIds
        val rebuildSelectedChapters = items.size > SELECTION_INCREMENTAL_UPDATE_LIMIT

        items.forEach { item ->
            processedIndexByChapterId[item.id]?.let { index ->
                updatedProcessedChapters = updatedProcessedChapters.replacingAt(index, item)
            }
            rowIndexByChapterId[item.id]?.let { index ->
                updatedRows = updatedRows.replacingAt(index, item)
            }
            if (!rebuildSelectedChapters) {
                val selectedIndex = updatedSelectedChapters.indexOfFirst { it.id == item.id }
                val processedIndex = processedIndexByChapterId[item.id]
                if (item.selected && processedIndex != null) {
                    updatedSelectedChapters = if (selectedIndex >= 0) {
                        updatedSelectedChapters.replacingAt(selectedIndex, item)
                    } else {
                        val insertionIndex = updatedSelectedChapters.indexOfFirst { selectedItem ->
                            processedIndexByChapterId.getValue(selectedItem.id) > processedIndex
                        }.takeIf { it >= 0 } ?: updatedSelectedChapters.size
                        updatedSelectedChapters.addingAt(insertionIndex, item)
                    }
                } else if (selectedIndex >= 0) {
                    updatedSelectedChapters = updatedSelectedChapters.removingAt(selectedIndex)
                }
            }
            updatedSelectedChapterIds = if (item.selected) {
                updatedSelectedChapterIds.adding(item.id)
            } else {
                updatedSelectedChapterIds.removing(item.id)
            }
        }

        if (rebuildSelectedChapters) {
            updatedSelectedChapters = updatedProcessedChapters
                .filter(EntryChapterList.Item::selected)
                .toPersistentList()
        }

        return copy(
            processedChapters = updatedProcessedChapters,
            rows = updatedRows,
            selectedChapters = updatedSelectedChapters,
            selectedChapterIds = updatedSelectedChapterIds,
        )
    }
}

data class EntryChapterProgress(
    val totalCount: Int,
    val unreadCount: Int,
)

private const val SELECTION_INCREMENTAL_UPDATE_LIMIT = 32

internal fun buildChapterPresentation(
    entry: Entry,
    chapters: List<EntryChapterList.Item>,
    memberIds: List<Long>,
    memberTitleById: Map<Long, String>,
    /** Only filled for a merge spanning several sources; a single source would repeat on every member header. */
    memberSourceNameById: Map<Long, String>,
    expandedMemberIds: Set<Long>,
    chapterGroups: List<EntryChapterGroup>,
    expandedGroupIds: Set<ChapterGroupId>,
    childListFeature: EntryChildListFeature,
    scanlatorFilterFeature: EntryScanlatorFilterFeature,
    scanlatorFilterState: EntryScanlatorFilterStateResult,
    hideMissingChapters: Boolean,
    downloadedOnly: Boolean,
): EntryChapterPresentation {
    val scanlatorFilteredChapters = when (scanlatorFilterState) {
        is EntryScanlatorFilterStateResult.Available -> {
            when (
                val filtered = scanlatorFilterFeature.filter(
                    entry = entry,
                    chapters = chapters.map { it.chapter },
                    excludedScanlators = scanlatorFilterState.state.excludedScanlators,
                )
            ) {
                is EntryScanlatorFilterResult.Available -> {
                    val visibleIds = filtered.chapters.mapTo(hashSetOf(), EntryChapter::id)
                    chapters.filter { it.chapter.id in visibleIds }
                }
                is EntryScanlatorFilterResult.Inapplicable -> chapters
            }
        }
        is EntryScanlatorFilterStateResult.Inapplicable -> chapters
    }
    val progress = EntryChapterProgress(
        totalCount = scanlatorFilteredChapters.size,
        unreadCount = scanlatorFilteredChapters.count { !it.chapter.read },
    )
    val hasFiller = chapters.any { it.chapter.kind == ChapterKind.FILLER }
    val hasPreviews = chapters.any { it.chapter.previewUrl != null || it.chapter.summary != null }
    val filteredChapters = scanlatorFilteredChapters.applyFilters(entry, downloadedOnly)
    val itemByChapterId = filteredChapters.associateBy { it.chapter.id }
    val result = childListFeature.displayList(
        EntryChildListRequest(
            entry = entry,
            chapters = filteredChapters.map(EntryChapterList.Item::chapter),
            memberIds = memberIds,
            memberTitleById = memberTitleById,
            includeMissingCounts = !hideMissingChapters,
            chapterGroups = chapterGroups,
            allChapters = chapters.map(EntryChapterList.Item::chapter),
        ),
    )
    if (result is EntryChildListResult.Inapplicable) {
        return EntryChapterPresentation(
            processedChapters = persistentListOf(),
            rows = persistentListOf(),
            selectedChapters = persistentListOf(),
            aggregateMissingCount = 0,
            hasUnreadChapters = false,
            hasReadChapters = chapters.any { it.chapter.read },
            progress = progress,
            hasFiller = hasFiller,
            hasPreviews = hasPreviews,
            rawIndexByChapterId = chapters.indexByChapterId(),
            processedIndexByChapterId = emptyMap(),
            rowIndexByChapterId = emptyMap(),
            selectedChapterIds = chapters.selectedChapterIds(),
            sourceIds = chapters.sourceIds(),
        )
    }
    val display = (result as EntryChildListResult.Available).display
    val chaptersByMember = filteredChapters.groupBy { it.chapter.entryId }
    val chaptersByGroup = filteredChapters.groupBy { it.chapter.groupId }
    // Rows after a collapsed member's header belong to that member until the next member header, and rows after a
    // collapsed group's header to that group until the next header of either kind.
    var inCollapsedMember = false
    var inCollapsedGroup = false
    val rows = display.rows.mapNotNull { row ->
        when (row) {
            is EntryChildListRow.Child -> itemByChapterId[row.chapter.id]?.takeUnless {
                inCollapsedMember || inCollapsedGroup
            }
            is EntryChildListRow.MemberHeader -> {
                val memberChapters = chaptersByMember[row.entryId].orEmpty()
                val expanded = row.entryId in expandedMemberIds
                inCollapsedMember = !expanded
                inCollapsedGroup = false
                EntryChapterList.MemberHeader(
                    entryId = row.entryId,
                    title = row.title,
                    sourceName = memberSourceNameById[row.entryId],
                    childCount = memberChapters.size,
                    unreadCount = memberChapters.count { !it.chapter.read },
                    expanded = expanded,
                )
            }
            is EntryChildListRow.GroupHeader -> {
                val group = ChapterGroupId(row.entryId, row.groupKey)
                val groupChapters = chaptersByGroup[group].orEmpty()
                val expanded = group in expandedGroupIds
                inCollapsedGroup = !expanded
                EntryChapterList.GroupHeader(
                    group = group,
                    title = row.title,
                    childCount = groupChapters.size,
                    unreadCount = groupChapters.count { !it.chapter.read },
                    expanded = expanded,
                    nested = memberIds.size > 1,
                ).takeUnless { inCollapsedMember }
            }
            is EntryChildListRow.MissingCount -> EntryChapterList.MissingCount(
                id = row.id,
                count = row.count,
            ).takeUnless { inCollapsedMember || inCollapsedGroup }
        }
    }.toPersistentList()
    val processedChapters = display.rows.mapNotNull { row ->
        (row as? EntryChildListRow.Child)?.let { itemByChapterId[it.chapter.id] }
    }.toPersistentList()
    return EntryChapterPresentation(
        processedChapters = processedChapters,
        rows = rows,
        selectedChapters = processedChapters.filter(EntryChapterList.Item::selected).toPersistentList(),
        aggregateMissingCount = display.aggregateMissingCount,
        hasUnreadChapters = processedChapters.any { !it.chapter.read },
        hasReadChapters = chapters.any { it.chapter.read },
        progress = progress,
        hasFiller = hasFiller,
        hasPreviews = hasPreviews,
        rawIndexByChapterId = chapters.indexByChapterId(),
        processedIndexByChapterId = processedChapters.indexByChapterId(),
        rowIndexByChapterId = rows.mapIndexedNotNull { index, row ->
            (row as? EntryChapterList.Item)?.let { it.id to index }
        }.toMap(),
        selectedChapterIds = chapters.selectedChapterIds(),
        sourceIds = chapters.sourceIds(),
    )
}

private fun List<EntryChapterList.Item>.indexByChapterId(): Map<Long, Int> =
    mapIndexed { index, item -> item.id to index }.toMap()

private fun List<EntryChapterList.Item>.selectedChapterIds(): PersistentSet<Long> =
    asSequence()
        .filter(EntryChapterList.Item::selected)
        .map(EntryChapterList.Item::id)
        .toPersistentSet()

private fun List<EntryChapterList.Item>.sourceIds(): PersistentSet<Long> =
    asSequence()
        .map { it.entry.source }
        .toPersistentSet()

internal fun List<EntryChapterList.Item>.applyFilters(
    entry: Entry,
    downloadedOnly: Boolean,
): List<EntryChapterList.Item> {
    val filters = EntryChapterFilters(entry, downloadedOnly)
    val isLocalEntry = entry.isLocal()
    return filter { item -> filters.shows(item.chapter) { item.isDownloaded || isLocalEntry } }
}
