package tachiyomi.domain.entry.service

import eu.kanade.tachiyomi.source.entry.ChapterKind
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.util.applyFilter

/**
 * The filters an entry's chapter list applies. Continue, the readers' "skip filtered" and the player's next and
 * previous episode follow the same ones, so none of them lands on a chapter the list hides.
 *
 * [downloadedOnly] is the global "Downloaded only" setting, which turns the Downloaded filter on for every entry.
 */
class EntryChapterFilters(entry: Entry, downloadedOnly: Boolean) {
    val unread: TriState = entry.unreadFilter
    val downloaded: TriState = if (downloadedOnly) TriState.ENABLED_IS else entry.downloadedFilter
    val bookmarked: TriState = entry.bookmarkedFilter
    val filler: TriState = entry.fillerFilter

    val active: Boolean
        get() = listOf(unread, downloaded, bookmarked, filler).any { it != TriState.DISABLED }

    /** [isDownloaded] is asked only while the Downloaded filter is on; a local entry's chapters count as downloaded. */
    fun shows(chapter: EntryChapter, isDownloaded: () -> Boolean): Boolean {
        return applyFilter(unread) { !chapter.read } &&
            applyFilter(bookmarked) { chapter.bookmark } &&
            applyFilter(filler) { chapter.kind == ChapterKind.FILLER } &&
            applyFilter(downloaded, isDownloaded)
    }
}
