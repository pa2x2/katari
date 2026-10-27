package tachiyomi.domain.entry.service

/** Starts reading the older parts of chapter lists in the background, for every entry whose walk goes on. */
fun interface ChapterListWalkScheduler {
    /**
     * [restart] stops a run waiting to retry, as the user's Retry does; otherwise the reading starts after any current
     * run, so a walk started while one runs is never missed.
     */
    fun schedule(restart: Boolean)
}
