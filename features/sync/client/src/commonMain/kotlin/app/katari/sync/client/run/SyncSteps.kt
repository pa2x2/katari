package app.katari.sync.client.run

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

/**
 * The step of the runs under way, for the status to name with its progress: a bare "Syncing" through minutes of
 * uploading a large library reads as a hang. Steps are named from [shownFrom], two seconds into a run: a shorter run
 * reads as one plain sync, and a longer one names every step from then on.
 *
 * The step last named stays until another reports how far it got, so the work between steps, the end of the run and
 * the runs that follow it within [RUNS_SETTLE] read as part of it rather than as a bare "Syncing" (D064). A run that
 * starts after that starts over.
 */
@Inject
@SingleIn(AppScope::class)
class SyncSteps {
    private val current = MutableStateFlow<SyncStep?>(null)
    val step: StateFlow<SyncStep?> = current.asStateFlow()

    /** Epoch milliseconds from which the steps of the runs under way are named. */
    @Volatile
    var shownFrom: Long = 0
        private set

    @Volatile
    private var lastEnded = 0L

    /** A run starts working, before it shows as running. */
    internal fun begin() {
        val now = nowMillis()
        if (now - lastEnded <= RUNS_SETTLE.inWholeMilliseconds) return
        current.value = null
        shownFrom = now + QUIET.inWholeMilliseconds
    }

    /** The run that [begin] started ended. */
    internal fun end() {
        lastEnded = nowMillis()
    }

    /**
     * Runs [block] as a step of [kind], which reports how far it got, as done of total, to the function it is given. A
     * step with nothing to do leaves the last one named.
     */
    internal suspend fun <T> step(kind: SyncStep.Kind, block: suspend (progress: (Long, Long) -> Unit) -> T): T =
        block(progressOf(kind))

    /**
     * Names a step of [kind] with how far it got, done of total, for work that happens inside another step, such as the
     * files a record push waits for; the other step is named again as soon as it reports.
     */
    internal fun progressOf(kind: SyncStep.Kind): (done: Long, total: Long) -> Unit = { done, total ->
        if (total > 0) current.value = SyncStep(kind, done.coerceIn(0, total), total)
    }

    private fun nowMillis() = Clock.System.now().toEpochMilliseconds()

    private companion object {
        val QUIET = 2.seconds
    }
}
