package eu.kanade.tachiyomi.ui.entry

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import mihon.entry.interactions.child.EntryChildGroupFilterFeature
import mihon.entry.interactions.child.EntryChildGroupFilterResult
import mihon.entry.interactions.child.EntryChildGroupFilterStateResult
import mihon.entry.interactions.child.EntryChildListFeature
import mihon.entry.interactions.child.EntryChildListRequest
import mihon.entry.interactions.child.EntryChildListResult
import mihon.entry.interactions.child.EntryChildListRow
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.util.applyFilter

data class EntryChapterPresentation(
    val processedChapters: PersistentList<EntryChapterList.Item>,
    val rows: PersistentList<EntryChapterList>,
    val selectedChapters: PersistentList<EntryChapterList.Item>,
    val aggregateMissingCount: Int,
    val hasUnreadChapters: Boolean,
    val hasReadChapters: Boolean,
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

private const val SELECTION_INCREMENTAL_UPDATE_LIMIT = 32

internal fun buildChapterPresentation(
    entry: Entry,
    chapters: List<EntryChapterList.Item>,
    memberIds: List<Long>,
    memberTitleById: Map<Long, String>,
    childListFeature: EntryChildListFeature,
    childGroupFilterFeature: EntryChildGroupFilterFeature,
    childGroupFilterState: EntryChildGroupFilterStateResult,
    hideMissingChapters: Boolean,
): EntryChapterPresentation {
    val groupFilteredChapters = when (childGroupFilterState) {
        is EntryChildGroupFilterStateResult.Available -> {
            when (
                val filtered = childGroupFilterFeature.filter(
                    entry = entry,
                    chapters = chapters.map { it.chapter },
                    excludedGroups = childGroupFilterState.state.excludedGroups,
                )
            ) {
                is EntryChildGroupFilterResult.Available -> {
                    val visibleIds = filtered.chapters.mapTo(hashSetOf(), EntryChapter::id)
                    chapters.filter { it.chapter.id in visibleIds }
                }
                is EntryChildGroupFilterResult.Inapplicable -> chapters
            }
        }
        is EntryChildGroupFilterStateResult.Inapplicable -> chapters
    }
    val filteredChapters = groupFilteredChapters.applyFilters(entry)
    val itemByChapterId = filteredChapters.associateBy { it.chapter.id }
    val result = childListFeature.displayList(
        EntryChildListRequest(
            entry = entry,
            chapters = filteredChapters.map(EntryChapterList.Item::chapter),
            memberIds = memberIds,
            memberTitleById = memberTitleById,
            includeMissingCounts = !hideMissingChapters,
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
            rawIndexByChapterId = chapters.indexByChapterId(),
            processedIndexByChapterId = emptyMap(),
            rowIndexByChapterId = emptyMap(),
            selectedChapterIds = chapters.selectedChapterIds(),
            sourceIds = chapters.sourceIds(),
        )
    }
    val display = (result as EntryChildListResult.Available).display
    val rows = display.rows.mapNotNull { row ->
        when (row) {
            is EntryChildListRow.Child -> itemByChapterId[row.chapter.id]
            is EntryChildListRow.MemberHeader -> EntryChapterList.MemberHeader(
                entryId = row.entryId,
                title = row.title,
            )
            is EntryChildListRow.MissingCount -> EntryChapterList.MissingCount(
                id = row.id,
                count = row.count,
            )
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

private fun List<EntryChapterList.Item>.applyFilters(entry: Entry): List<EntryChapterList.Item> {
    val isLocalEntry = entry.isLocal()
    val unreadFilter = entry.unreadFilter
    val downloadedFilter = entry.downloadedFilter
    val bookmarkedFilter = entry.bookmarkedFilter
    return asSequence()
        .filter { (chapter) -> applyFilter(unreadFilter) { !chapter.read } }
        .filter { (chapter) -> applyFilter(bookmarkedFilter) { chapter.bookmark } }
        .filter { applyFilter(downloadedFilter) { it.isDownloaded || isLocalEntry } }
        .toList()
}
