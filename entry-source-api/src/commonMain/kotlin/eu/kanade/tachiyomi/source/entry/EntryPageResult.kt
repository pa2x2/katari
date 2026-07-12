package eu.kanade.tachiyomi.source.entry

/**
 * Generic pagination wrapper for unified source content lists.
 *
 * Page result returned by Entry-era catalogue APIs.
 */
data class EntryPageResult<T>(
    val items: List<T>,
    val hasNextPage: Boolean,
)
