package eu.kanade.presentation.library.settings

import dev.icerock.moko.resources.StringResource
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.i18n.*

/** What the library menu's Sort tab calls the sort. */
internal val LibrarySort.Type.label: StringResource
    get() = when (this) {
        LibrarySort.Type.Alphabetical -> MR.strings.action_sort_alpha
        LibrarySort.Type.TotalChapters -> MR.strings.action_sort_total
        LibrarySort.Type.LastRead -> MR.strings.action_sort_last_read
        LibrarySort.Type.LastUpdate -> MR.strings.action_sort_last_manga_update
        LibrarySort.Type.UnreadCount -> MR.strings.action_sort_unread_count
        LibrarySort.Type.LatestChapter -> MR.strings.action_sort_latest_chapter
        LibrarySort.Type.ChapterFetchDate -> MR.strings.action_sort_chapter_fetch_date
        LibrarySort.Type.DateAdded -> MR.strings.action_sort_date_added
        LibrarySort.Type.Random -> MR.strings.action_sort_random
    }
