package tachiyomi.domain.work

/**
 * How one pass of background work ended, which each platform's work runner acts on: Android's WorkManager workers
 * return it as their result, desktop's in-process work schedules the retry itself.
 */
enum class WorkOutcome {
    Done,

    /** Run the pass again after the work's backoff. */
    RetryLater,

    /** Give up; nothing runs the work again until it is started anew. */
    Failed,
}
