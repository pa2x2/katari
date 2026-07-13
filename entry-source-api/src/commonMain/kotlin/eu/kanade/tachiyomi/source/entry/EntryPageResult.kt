package eu.kanade.tachiyomi.source.entry

/**
 * Generic pagination wrapper for unified source content lists.
 *
 * Page result returned by Entry-era catalogue APIs.
 *
 * @property items items returned for this page.
 * @property hasNextPage whether requesting the next page can return more items.
 */
data class EntryPageResult<T>(
    val items: List<T>,
    val hasNextPage: Boolean,
)
