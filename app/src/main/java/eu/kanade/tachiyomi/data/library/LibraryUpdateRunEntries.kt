package eu.kanade.tachiyomi.data.library

import tachiyomi.domain.entry.model.Entry
import java.util.concurrent.ConcurrentHashMap

/**
 * The entries one library update checks: the ones it planned, and the members a source added to a merged entry while
 * the update refreshed it. Each entry is checked once, so a member that was already planned on its own isn't joined.
 */
internal class LibraryUpdateRunEntries(val planned: List<Entry>) {
    private val claimedIds = ConcurrentHashMap.newKeySet<Long>().apply { planned.mapTo(this, Entry::id) }

    val size: Int
        get() = claimedIds.size

    /** Adds the [members] this update doesn't check yet and returns them. */
    fun join(members: List<Entry>): List<Entry> {
        return members.filter { claimedIds.add(it.id) }
    }
}
