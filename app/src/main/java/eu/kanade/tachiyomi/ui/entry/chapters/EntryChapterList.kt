package eu.kanade.tachiyomi.ui.entry.chapters

import androidx.compose.runtime.Immutable
import mihon.entry.interactions.download.EntryDownloadState
import tachiyomi.domain.entry.model.ChapterGroupId
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

@Immutable
sealed class EntryChapterList {
    @Immutable
    data class MemberHeader(
        val entryId: Long,
        val title: String,
        /** Null when every member comes from one source. */
        val sourceName: String?,
        val childCount: Int,
        val unreadCount: Int,
        val expanded: Boolean,
    ) : EntryChapterList()

    /**
     * Heads one group of chapters; a null [title] is the chapters in no group, and a [childCount] of 0 a group with no
     * chapters yet. A [nested] header sits inside its merged member's card.
     */
    @Immutable
    data class GroupHeader(
        val group: ChapterGroupId,
        val title: String?,
        val childCount: Int,
        val unreadCount: Int,
        val expanded: Boolean,
        val nested: Boolean,
    ) : EntryChapterList()

    @Immutable
    data class MissingCount(
        val id: String,
        val count: Int,
    ) : EntryChapterList()

    @Immutable
    data class Item(
        val chapter: EntryChapter,
        val entry: Entry,
        val downloadState: EntryDownloadState,
        val selected: Boolean = false,
    ) : EntryChapterList() {
        val id = chapter.id
        val isDownloaded = downloadState == EntryDownloadState.DOWNLOADED
    }
}
