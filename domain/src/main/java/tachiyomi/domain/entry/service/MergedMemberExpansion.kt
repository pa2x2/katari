package tachiyomi.domain.entry.service

/**
 * Which members of a merged chapter list are expanded: the ones [saved] as expanded, plus the current member (the one
 * holding the next chapter to read, or the one playing) so the list opens on it. A member toggled while the list is open
 * follows only its own choice, so collapsing the current member works until the list is opened again. A toggle shows at
 * once and holds until [saved] catches up with it, so a second tap before the first is stored still flips it back.
 */
data class MergedMemberExpansion(
    private val saved: Set<Long> = emptySet(),
    private val toggled: Set<Long> = emptySet(),
    private val unsaved: Map<Long, Boolean> = emptyMap(),
) {
    fun shown(currentMemberId: Long?): Set<Long> {
        val candidates = saved + unsaved.keys + listOfNotNull(currentMemberId)
        return candidates.filterTo(mutableSetOf()) { memberId ->
            unsaved[memberId] ?: (memberId in saved || (memberId == currentMemberId && memberId !in toggled))
        }
    }

    fun withSaved(saved: Set<Long>): MergedMemberExpansion {
        return copy(saved = saved, unsaved = unsaved.filter { (memberId, expanded) -> (memberId in saved) != expanded })
    }

    /** Flips [memberId]; the result is what to store for it. */
    fun toggle(memberId: Long, currentMemberId: Long?): MergedMemberExpansion {
        val expanded = memberId !in shown(currentMemberId)
        return copy(toggled = toggled + memberId, unsaved = unsaved + (memberId to expanded))
    }
}
