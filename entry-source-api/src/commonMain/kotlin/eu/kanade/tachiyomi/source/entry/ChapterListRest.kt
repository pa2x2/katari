package eu.kanade.tachiyomi.source.entry

/**
 * What follows the chapters a source returned, for a site that lists a long entry's chapters over many pages.
 *
 * A chapter list may arrive in parts, newest first: [UnifiedSource.refresh] returns the newest part and
 * [ChapterListPartsSource.getOlderChapters] each older one. The host saves every part as it arrives and removes stored
 * chapters only once a walk through all parts ends with [Complete].
 */
sealed interface ChapterListRest {

    /** No older chapters follow: together with the parts before it, this is the whole list. */
    data object Complete : ChapterListRest

    /**
     * The older chapters are as the host has them: it keeps the stored chapters this part doesn't list, in their
     * order, and removes none. Answer it when [UnifiedSource.refresh]'s stored chapters show that nothing older
     * changed, so a library update reads only the newest part.
     */
    data object Unchanged : ChapterListRest

    /**
     * Older chapters follow; the host asks for them with [token] through [ChapterListPartsSource.getOlderChapters].
     *
     * @property token opaque to the host, which stores it so the walk resumes after a restart; keep it valid that long.
     * @property loaded how many of the [total] units (such as the site's pages) the walk has read so far, when known.
     * @property total how many units the whole list has, when known.
     */
    class More(
        val token: String,
        val loaded: Int? = null,
        val total: Int? = null,
    ) : ChapterListRest {
        init {
            require(token.isNotBlank()) { "a chapter list token must not be blank" }
            require(token.length <= MAX_TOKEN_LENGTH) {
                "a chapter list token must not exceed $MAX_TOKEN_LENGTH characters"
            }
            require(loaded == null || loaded >= 0) { "loaded must not be negative" }
            require(total == null || total >= 0) { "total must not be negative" }
        }

        companion object {
            const val MAX_TOKEN_LENGTH = 4_096
        }
    }
}
