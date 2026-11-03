package eu.kanade.tachiyomi.ui.library

/**
 * Whether the library offers its filter for entries outside their release period, which follows custom update
 * intervals; `LibraryUpdateAvailabilityBindings` decides it.
 */
data class ReleasePeriodFilterAvailability(val isOffered: Boolean)
