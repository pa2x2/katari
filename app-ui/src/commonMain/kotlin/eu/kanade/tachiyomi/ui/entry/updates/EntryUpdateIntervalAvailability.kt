package eu.kanade.tachiyomi.ui.entry.updates

/**
 * Whether an entry's update interval can be set by hand from its release estimate. Each platform's graph decides it
 * for the build it runs in.
 */
data class EntryUpdateIntervalAvailability(val isOffered: Boolean)
