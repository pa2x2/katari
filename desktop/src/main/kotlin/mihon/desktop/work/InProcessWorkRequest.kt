package mihon.desktop.work

import tachiyomi.domain.work.WorkBackoff
import tachiyomi.domain.work.WorkOutcome
import kotlin.time.Duration.Companion.seconds

/** What new work does to unfinished work of the same name, as WorkManager's policies of the same names. */
enum class ExistingWorkPolicy {
    /** Cancels the unfinished work and starts the new one. */
    Replace,

    /** Keeps the unfinished work and drops the new one. */
    Keep,

    /** Runs the new work after the unfinished work. */
    Append,
}

/**
 * Work for [InProcessWork] to run.
 *
 * @property resumeAs the [ResumableWork] kind that starts the work again with [input] at the next launch, when the app
 * quits before the work ends.
 * @property requiresNetwork each run waits until the network is online.
 * @property run one pass of the work. Throwing ends the work as [WorkOutcome.Failed].
 */
class InProcessWorkRequest(
    val resumeAs: String,
    val input: Map<String, String> = emptyMap(),
    val requiresNetwork: Boolean = false,
    val backoff: WorkBackoff = DEFAULT_BACKOFF,
    val run: suspend (InProcessWorkRun) -> WorkOutcome,
) {
    private companion object {
        // WorkManager's own default
        val DEFAULT_BACKOFF = WorkBackoff(initialDelay = 30.seconds)
    }
}

/**
 * One run of a work.
 *
 * @property attempt how many times the work ran before, 0 on its first run.
 */
class InProcessWorkRun(val id: String, val attempt: Int)

/**
 * Work that hasn't ended.
 *
 * @property queuedAt when it was added, in milliseconds since the epoch.
 * @property isRunning running a pass now, rather than waiting for its turn, the network or a retry.
 */
data class InProcessWorkInfo(
    val id: String,
    val name: String,
    val input: Map<String, String>,
    val queuedAt: Long,
    val isRunning: Boolean,
)
