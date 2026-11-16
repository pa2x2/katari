package app.katari.sync.client.run

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import kotlin.time.Duration.Companion.seconds

/**
 * Sync runs, one at a time per device. Every trigger [request]s one; a request made while a run is going is merged
 * with the others made meanwhile into the run that follows it, so triggers coalesce instead of queueing. Background
 * work that must know how its run ended waits with [runAndWait].
 *
 * Changing the connection itself (signing in, being removed) waits for the current run and keeps new ones from
 * starting with [exclusive], so a run never works on a connection taken from under it. Leaving the account cuts the
 * current run short instead ([interrupting]): what it would still do means nothing once the device has left, and a
 * first download or a full resync can take minutes.
 */
@Inject
@SingleIn(AppScope::class)
class SyncRuns(private val pass: SyncPass, private val steps: SyncSteps) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    private val runLock = Mutex()
    private var waiting: SyncRequest? = null
    private var worker: Job? = null
    private var requested = 0L

    // The run going now, and how many callers wait to cut runs short: a run the worker starts before such a caller
    // gets the lock is cut short as soon as it starts
    private var current: Job? = null
    private var interrupters = 0

    // Each waiting caller has its own run's outcome handed to it: the latest outcome alone can be a later run's, as
    // when the writes of the run it waited for start another that finds nothing to do
    private val waiters = ArrayList<Waiter>()
    private val finishedThrough = MutableStateFlow(0L)
    private val endings = MutableSharedFlow<SyncOutcome>(extraBufferCapacity = ENDINGS_BUFFER)
    private val runningState = MutableStateFlow(false)
    private val outcomes = MutableStateFlow(SyncOutcome.IDLE)

    /** Whether a run is going now that has work to do with the server ([SyncPass.run]). */
    val running: StateFlow<Boolean> = runningState.asStateFlow()

    /**
     * How the last run that had something to do ended; [SyncOutcome.IDLE] before the first. A run that found nothing
     * to do leaves it, so a failure stays reported until a run gets through.
     */
    val lastOutcome: StateFlow<SyncOutcome> = outcomes.asStateFlow()

    /**
     * How each run ends from now on, IDLE ones too. A collector that falls behind hears every one, as one that left
     * work must be retried even when a run that found nothing to do ends right after it.
     */
    val ends: SharedFlow<SyncOutcome> = endings.asSharedFlow()

    fun request(request: SyncRequest = SyncRequest()) {
        enqueue(request)
    }

    /** Requests a run and waits for the run that carries it, returning how it ended. */
    suspend fun runAndWait(request: SyncRequest = SyncRequest()): SyncOutcome {
        val outcome = CompletableDeferred<SyncOutcome>()
        synchronized(lock) { waiters += Waiter(enqueue(request), outcome) }
        return outcome.await()
    }

    /**
     * Waits until every run requested so far has ended, and those requested meanwhile too. [running] doesn't tell: it
     * turns on only once a run has work to do, after the run has already read the database to decide.
     */
    internal suspend fun awaitIdle() {
        while (true) {
            val last = synchronized(lock) { requested }
            finishedThrough.first { it >= last }
            if (synchronized(lock) { requested } == last) return
        }
    }

    /** Runs [block] with no run going, after the current one ends. */
    suspend fun <T> exclusive(block: suspend () -> T): T = runLock.withLock { block() }

    /**
     * Runs [block] with no run going, cutting the current one short rather than waiting for it to end. Callers waiting
     * for that run hear [SyncOutcome.IDLE]; runs requested meanwhile start after [block].
     */
    suspend fun <T> interrupting(block: suspend () -> T): T {
        synchronized(lock) {
            interrupters++
            current?.cancel()
        }
        var waiting = true
        try {
            return runLock.withLock {
                synchronized(lock) { interrupters-- }
                waiting = false
                block()
            }
        } finally {
            if (waiting) synchronized(lock) { interrupters-- }
        }
    }

    private fun enqueue(request: SyncRequest): Long = synchronized(lock) {
        waiting = waiting?.merge(request) ?: request
        val ticket = ++requested
        if (worker?.isActive != true) worker = scope.launch { work() }
        ticket
    }

    private suspend fun work() {
        while (true) {
            val (next, ticket) = synchronized(lock) {
                val next = waiting ?: run {
                    worker = null
                    return
                }
                waiting = null
                next to requested
            }
            var working = false
            val outcome = try {
                runLock.withLock {
                    cuttable {
                        pass.run(next) {
                            working = true
                            steps.begin()
                            runningState.value = true
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Sync run failed" }
                SyncOutcome.FAILED
            } finally {
                if (working) steps.end()
                runningState.value = synchronized(lock) { waiting != null }
            }
            if (outcome != SyncOutcome.IDLE) outcomes.value = outcome
            val carried = synchronized(lock) {
                waiters.filter { it.ticket <= ticket }.also { waiters.removeAll(it.toSet()) }
            }
            carried.forEach { it.outcome.complete(outcome) }
            finishedThrough.value = ticket
            endings.emit(outcome)
        }
    }

    /** Runs [run] so that [interrupting] can cut it short, which ends it [SyncOutcome.IDLE]. */
    private suspend fun cuttable(run: suspend () -> SyncOutcome): SyncOutcome = coroutineScope {
        val job = async { run() }
        synchronized(lock) {
            current = job
            if (interrupters > 0) job.cancel()
        }
        try {
            job.await()
        } catch (e: CancellationException) {
            // The worker itself stopping is no run cut short
            if (!currentCoroutineContext().isActive) throw e
            SyncOutcome.IDLE
        } finally {
            synchronized(lock) { current = null }
        }
    }

    private class Waiter(val ticket: Long, val outcome: CompletableDeferred<SyncOutcome>)

    private companion object {
        // A collector this many endings behind holds up the next run's end until it catches up
        const val ENDINGS_BUFFER = 16
    }
}

/**
 * How long runs count as going after one ends, so runs that follow each other read as one and the status doesn't
 * flicker to up to date between them (D064).
 */
internal val RUNS_SETTLE = 3.seconds
