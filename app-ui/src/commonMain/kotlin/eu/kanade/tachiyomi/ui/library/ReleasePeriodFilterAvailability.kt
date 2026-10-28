package eu.kanade.tachiyomi.ui.library

/**
 * Whether the library offers its filter for entries outside their release period, which follows custom update
 * intervals. Each platform's graph decides it for the build it runs in.
 */
data class ReleasePeriodFilterAvailability(val isOffered: Boolean)
