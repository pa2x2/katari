package mihon.entry.interactions.navigation

import mihon.entry.interactions.download.EntryDownloadRuntimeFeature
import mihon.entry.interactions.download.EntryDownloadState
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.service.EntryChapterFilters
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.identity.LOCAL_SOURCE_ID

/**
 * Narrows what Continue picks from to the chapters the entry's list shows, so neither saved progress nor the first
 * unread chapter can land on one the list's filters hide. Every Continue path, single or batched, passes through here.
 */
internal class EntryContinueChapterVisibility(
    private val libraryPreferences: LibraryPreferences,
    private val downloadRuntime: EntryDownloadRuntimeFeature,
) {
    /**
     * [listOwner] is the entry whose list is shown, the target for a merge, so its filters apply to every member's
     * chapters. [owners] are the entries the chapters belong to.
     */
    fun shown(listOwner: Entry, owners: List<Entry>, input: EntryContinuePreparedInput): EntryContinuePreparedInput {
        val filters = EntryChapterFilters(listOwner, libraryPreferences.downloadedOnly.get())
        if (!filters.active) return input
        val downloadedIds = if (filters.downloaded != TriState.DISABLED) {
            downloadedChapterIds(owners, input.chapters)
        } else {
            emptySet()
        }
        val chapters = input.chapters.filter { chapter -> filters.shows(chapter) { chapter.id in downloadedIds } }
        val chapterIds = chapters.mapTo(hashSetOf(), EntryChapter::id)
        return EntryContinuePreparedInput(
            chapters = chapters,
            progressStates = input.progressStates.filter { it.chapterId == null || it.chapterId in chapterIds },
        )
    }

    private fun downloadedChapterIds(owners: List<Entry>, chapters: List<EntryChapter>): Set<Long> {
        val ownerById = owners.associateBy(Entry::id)
        return chapters.groupBy(EntryChapter::entryId).flatMapTo(hashSetOf()) { (ownerId, ownerChapters) ->
            val owner = ownerById[ownerId] ?: return@flatMapTo emptyList()
            if (owner.source == LOCAL_SOURCE_ID) return@flatMapTo ownerChapters.map(EntryChapter::id)
            downloadRuntime.statuses(owner, ownerChapters).orEmpty()
                .filterValues { it.state == EntryDownloadState.DOWNLOADED }
                .keys
        }
    }
}
