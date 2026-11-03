package eu.kanade.tachiyomi.ui.entry.updates

/**
 * Whether an entry's update interval can be set by hand from its release estimate; `LibraryUpdateAvailabilityBindings`
 * decides it.
 */
data class EntryUpdateIntervalAvailability(val isOffered: Boolean)
