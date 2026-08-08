package eu.kanade.tachiyomi.ui.entry

import androidx.compose.ui.util.fastAny
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
    val processedChapters: List<EntryChapterList.Item>,
    val rows: List<EntryChapterList>,
    val aggregateMissingCount: Int,
    val isAnySelected: Boolean,
)

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
            processedChapters = emptyList(),
            rows = emptyList(),
            aggregateMissingCount = 0,
            isAnySelected = chapters.fastAny { it.selected },
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
    }
    return EntryChapterPresentation(
        processedChapters = display.rows.mapNotNull { row ->
            (row as? EntryChildListRow.Child)?.let { itemByChapterId[it.chapter.id] }
        },
        rows = rows,
        aggregateMissingCount = display.aggregateMissingCount,
        isAnySelected = chapters.fastAny { it.selected },
    )
}

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
