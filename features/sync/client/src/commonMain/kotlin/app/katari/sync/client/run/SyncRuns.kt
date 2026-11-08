package app.katari.sync.client.run

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
 * Changing the connection itself (signing out, being removed) waits for the current run and keeps new ones from
 * starting with [exclusive], so a run never works on a connection taken from under it.
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
    private val finished = MutableStateFlow(Finished(0, SyncOutcome.IDLE))
    private val runningState = MutableStateFlow(false)
    private val outcomes = MutableStateFlow(SyncOutcome.IDLE)

    /** Whether a run is going now that has work to do with the server ([SyncPass.run]). */
    val running: StateFlow<Boolean> = runningState.asStateFlow()

    /**
     * How the last run that had something to do ended; [SyncOutcome.IDLE] before the first. A run that found nothing
     * to do leaves it, so a failure stays reported until a run gets through.
     */
    val lastOutcome: StateFlow<SyncOutcome> = outcomes.asStateFlow()

    fun request(request: SyncRequest = SyncRequest()) {
        enqueue(request)
    }

    /** Requests a run and waits for the run that carries it, returning how it ended. */
    suspend fun runAndWait(request: SyncRequest = SyncRequest()): SyncOutcome {
        val ticket = enqueue(request)
        return finished.first { it.ticket >= ticket }.outcome
    }

    /** Runs [block] with no run going, after the current one ends. */
    suspend fun <T> exclusive(block: suspend () -> T): T = runLock.withLock { block() }

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
                    pass.run(next) {
                        working = true
                        steps.begin()
                        runningState.value = true
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
            finished.value = Finished(ticket, outcome)
        }
    }

    private class Finished(val ticket: Long, val outcome: SyncOutcome)
}

/**
 * How long runs count as going after one ends, so runs that follow each other read as one and the status doesn't
 * flicker to up to date between them (D064).
 */
internal val RUNS_SETTLE = 3.seconds
