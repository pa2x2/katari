package eu.kanade.tachiyomi.ui.entry

/**
 * Members whose chapters a merged chapter list shows: the ones saved as expanded, plus the member holding the next
 * chapter to read so the list opens on it. A member toggled while the screen is open follows only the saved choice,
 * so collapsing the current member works until the screen is opened again.
 */
internal fun shownExpandedMembers(saved: Set<Long>, toggledThisSession: Set<Long>, continueMemberId: Long?): Set<Long> {
    return if (continueMemberId == null || continueMemberId in toggledThisSession) saved else saved + continueMemberId
}

internal fun List<EntryChapterList.Item>.memberOf(chapterId: Long?): Long? {
    return chapterId?.let { id -> firstOrNull { it.id == id }?.chapter?.entryId }
}
