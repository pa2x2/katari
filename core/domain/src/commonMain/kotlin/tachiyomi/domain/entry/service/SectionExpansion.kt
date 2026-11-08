package tachiyomi.domain.entry.service

/**
 * Which sections of a chapter list are open, such as the members of a merge or the groups of chapters: the ones [saved]
 * as open, plus the current one (holding the next chapter to read, or the one playing) so the list opens on it. A
 * section toggled while the list is open follows only its own choice, so closing the current one works until the list
 * is opened again. A toggle shows at once and holds until [saved] catches up with it, so a second tap before the first
 * is stored still flips it back.
 */
data class SectionExpansion<K>(
    private val saved: Set<K> = emptySet(),
    private val toggled: Set<K> = emptySet(),
    private val unsaved: Map<K, Boolean> = emptyMap(),
) {
    fun shown(current: K?): Set<K> {
        val candidates = saved + unsaved.keys + listOfNotNull(current)
        return candidates.filterTo(mutableSetOf()) { section ->
            unsaved[section] ?: (section in saved || (section == current && section !in toggled))
        }
    }

    fun withSaved(saved: Set<K>): SectionExpansion<K> {
        return copy(saved = saved, unsaved = unsaved.filter { (section, expanded) -> (section in saved) != expanded })
    }

    /** Flips [section]; the result is what to store for it. */
    fun toggle(section: K, current: K?): SectionExpansion<K> {
        val expanded = section !in shown(current)
        return copy(toggled = toggled + section, unsaved = unsaved + (section to expanded))
    }
}
