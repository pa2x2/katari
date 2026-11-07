package app.katari.sync.client.run

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

/**
 * The step of the run under way, for the status to name with its progress: a bare "Syncing" through minutes of
 * uploading a large library reads as a hang. Steps are named from [shownFrom], two seconds into the run: a shorter run
 * reads as one plain sync, and a longer one names every step from then on, so it never drops back to a bare "Syncing"
 * between them.
 */
@Inject
@SingleIn(AppScope::class)
class SyncSteps {
    private val current = MutableStateFlow<SyncStep?>(null)
    val step: StateFlow<SyncStep?> = current.asStateFlow()

    /** Epoch milliseconds from which the steps of the run under way are named. */
    @Volatile
    var shownFrom: Long = 0
        private set

    /** Runs [block] as one run, whose steps are named once it has lasted [QUIET]. */
    internal suspend fun <T> run(block: suspend () -> T): T {
        shownFrom = Clock.System.now().toEpochMilliseconds() + QUIET.inWholeMilliseconds
        try {
            return block()
        } finally {
            current.value = null
        }
    }

    /** Runs [block] as a step of [kind], which reports how far it got, as done of total, to the function it is given. */
    internal suspend fun <T> step(kind: SyncStep.Kind, block: suspend (progress: (Long, Long) -> Unit) -> T): T {
        current.value = null
        try {
            return block { done, total -> current.value = SyncStep(kind, done.coerceIn(0, total), total) }
        } finally {
            current.update { if (it?.kind == kind) null else it }
        }
    }

    private companion object {
        val QUIET = 2.seconds
    }
}
