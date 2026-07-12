package eu.kanade.tachiyomi.data.track

import eu.kanade.tachiyomi.data.track.model.TrackSearch
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.track.model.EntryTrack

/**
 * A tracker that will never prompt the user to manually bind an entry.
 * It is expected that such tracker can only work with specific sources and unique IDs.
 */
interface EnhancedTracker {

    /**
     * This tracker will only work with the sources that are accepted by this filter function.
     */
    fun accept(source: EntryTrackingSource): Boolean {
        return source.legacyClassName in getAcceptedSources()
    }

    /**
     * Fully qualified source classes that this tracker is compatible with.
     */
    fun getAcceptedSources(): List<String>

    fun loginNoop()

    /**
     * Similar to [Tracker].search, but only returns zero or one match.
     */
    suspend fun match(entry: Entry): TrackSearch?

    /**
     * Checks whether the provided source/track/entry triplet is from this [Tracker]
     */
    fun isTrackFrom(track: EntryTrack, entry: Entry, source: EntryTrackingSource?): Boolean

    /**
     * Migrates the given track for the entry to the newSource, if possible
     */
    fun migrateTrack(track: EntryTrack, entry: Entry, newSource: EntryTrackingSource): EntryTrack?
}
