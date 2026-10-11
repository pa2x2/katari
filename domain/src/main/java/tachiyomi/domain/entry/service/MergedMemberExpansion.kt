package tachiyomi.domain.entry.service

/**
 * Members whose chapters a merged chapter list shows: the ones saved as expanded, plus the current member (the one
 * holding the next chapter to read, or the one playing) so the list opens on it. A member toggled while the list is
 * open follows only the saved choice, so collapsing the current member works until the list is opened again.
 */
fun shownExpandedMembers(saved: Set<Long>, toggledThisSession: Set<Long>, currentMemberId: Long?): Set<Long> {
    return if (currentMemberId == null || currentMemberId in toggledThisSession) saved else saved + currentMemberId
}
