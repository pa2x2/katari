package tachiyomi.domain.work

import kotlin.time.Duration

/**
 * How long background work waits before it runs again after a [WorkOutcome.RetryLater], as WorkManager counts it: the
 * [initialDelay] after the first run, then doubled after each later run, or grown by it when [exponential] is false.
 */
data class WorkBackoff(val initialDelay: Duration, val exponential: Boolean = true)
