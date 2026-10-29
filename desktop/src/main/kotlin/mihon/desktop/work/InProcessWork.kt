package mihon.desktop.work

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import logcat.LogPriority
import mihon.core.metro.AppCoroutineScope
import mihon.core.platform.network.NetworkConnectivity
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.work.WorkBackoff
import tachiyomi.domain.work.WorkOutcome
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Desktop's background work, run in the application's scope while the app is open: what WorkManager does on Android,
 * for the policies and constraints the app uses.
 *
 * Work has a unique name, and work of one name runs one at a time in the order it was added. A run waits for the
 * network when its request needs one, and a run that asks to be retried runs again after the request's backoff.
 * Periodic schedules run their work at an interval counted from the start of the previous run; a run the app missed
 * while it was closed runs once at the next launch. [PersistedWork] keeps what is pending and when each schedule last
 * ran, and [PersistedWorkResume] starts the work again at the next launch.
 */
@Inject
@SingleIn(AppScope::class)
class InProcessWork(
    @AppCoroutineScope private val scope: CoroutineScope,
    private val network: NetworkConnectivity,
    private val persisted: PersistedWork,
) {
    private val lock = Any()

    /** Work that hasn't ended, in the order it was added. */
    private val queued = mutableListOf<QueuedWork>()
    private val schedules = mutableMapOf<String, PeriodicWork>()
    private val infos = MutableStateFlow<List<InProcessWorkInfo>>(emptyList())
    private val nextRuns = MutableStateFlow<Map<String, Long>>(emptyMap())
    private val runningPasses = MutableStateFlow(0)

    /** Whether some work is running a pass now, which quitting would cut short. */
    val isWorking: Boolean
        get() = runningPasses.value > 0

    /** The work named [name] that hasn't ended. */
    fun works(name: String): Flow<List<InProcessWorkInfo>> {
        return infos.map { all -> all.filter { it.name == name } }.distinctUntilChanged()
    }

    fun currentWorks(name: String): List<InProcessWorkInfo> = infos.value.filter { it.name == name }

    /** Adds [request] as work named [name], or keeps the work already named so, as [policy] says; returns its id. */
    fun enqueue(name: String, policy: ExistingWorkPolicy, request: InProcessWorkRequest): String = synchronized(lock) {
        // Waiting work goes first, so cancelling the running one doesn't start the next one to cancel.
        val existing = queued.filter { it.name == name }.sortedBy { it.job != null }
        if (policy == ExistingWorkPolicy.Keep && existing.isNotEmpty()) return existing.first().id
        if (policy == ExistingWorkPolicy.Replace) existing.forEach(::cancelLocked)
        val work = QueuedWork(UUID.randomUUID().toString(), name, request, nowMillis())
        queued += work
        persisted.add(PersistedWork.PendingWork(work.id, request.resumeAs, request.input, work.queuedAt))
        startNextLocked(name)
        publishLocked()
        work.id
    }

    fun cancel(name: String) {
        synchronized(lock) {
            queued.filter { it.name == name }.sortedBy { it.job != null }.forEach(::cancelLocked)
            publishLocked()
        }
    }

    fun cancelById(id: String) {
        synchronized(lock) {
            queued.firstOrNull { it.id == id }?.let(::cancelLocked)
            publishLocked()
        }
    }

    /** [cancel], then waits until the work's runs have stopped. */
    suspend fun cancelAndJoin(name: String) {
        val jobs = synchronized(lock) {
            val works = queued.filter { it.name == name }.sortedBy { it.job != null }
            works.forEach(::cancelLocked)
            publishLocked()
            works.mapNotNull(QueuedWork::job)
        }
        jobs.joinAll()
    }

    /**
     * Runs [request] every [interval] as the schedule [name]. A schedule of that name that already runs keeps its last
     * run, so only the interval and the work change, and a run in progress goes on.
     */
    fun schedulePeriodic(name: String, interval: Duration, request: InProcessWorkRequest) {
        synchronized(lock) {
            persisted.putSchedule(name, request.resumeAs, request.input)
            val config = PeriodicConfig(interval, request)
            val schedule = schedules[name]
            if (schedule != null) {
                schedule.config.value = config
            } else {
                schedules[name] = PeriodicWork(name, config).also(PeriodicWork::start)
            }
        }
    }

    fun cancelPeriodic(name: String) {
        synchronized(lock) {
            schedules.remove(name)?.job?.cancel()
            persisted.removeSchedule(name)
            nextRuns.update { it - name }
        }
    }

    /** When the schedule [name] runs next, in milliseconds since the epoch; null while there is no such schedule. */
    fun nextRunAt(name: String): Flow<Long?> = nextRuns.map { it[name] }.distinctUntilChanged()

    private fun cancelLocked(work: QueuedWork) {
        work.cancelled = true
        val job = work.job
        if (job == null) endLocked(work) else job.cancel()
    }

    private fun startNextLocked(name: String) {
        if (queued.any { it.name == name && it.job != null }) return
        val next = queued.firstOrNull { it.name == name } ?: return
        val job = scope.launch { runPasses(next.id, next.request) { running -> setRunning(next, running) } }
        next.job = job
        job.invokeOnCompletion { cause ->
            synchronized(lock) {
                // Cancelled by the app's scope ending rather than by a caller, it is still pending for the next launch.
                val pending = cause is CancellationException && !next.cancelled
                if (pending) queued.remove(next) else endLocked(next)
                startNextLocked(name)
                publishLocked()
            }
        }
    }

    private fun endLocked(work: QueuedWork) {
        queued.remove(work)
        persisted.remove(work.id)
    }

    private fun setRunning(work: QueuedWork, running: Boolean) {
        synchronized(lock) {
            work.isRunning = running
            publishLocked()
        }
    }

    private fun publishLocked() {
        infos.value = queued.map { InProcessWorkInfo(it.id, it.name, it.request.input, it.queuedAt, it.isRunning) }
    }

    /** Runs passes of [request] until one doesn't ask to be retried. */
    private suspend fun runPasses(id: String, request: InProcessWorkRequest, onRunning: (Boolean) -> Unit = {}) {
        var attempt = 0
        while (true) {
            if (request.requiresNetwork) network.state().first { it.isOnline }
            onRunning(true)
            runningPasses.update { it + 1 }
            val outcome = try {
                request.run(InProcessWorkRun(id, attempt))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Background work ${request.resumeAs} failed" }
                WorkOutcome.Failed
            } finally {
                runningPasses.update { it - 1 }
                onRunning(false)
            }
            if (outcome != WorkOutcome.RetryLater) return
            delay(request.backoff.delayAfter(attempt))
            attempt++
        }
    }

    private class QueuedWork(val id: String, val name: String, val request: InProcessWorkRequest, val queuedAt: Long) {
        var job: Job? = null
        var isRunning = false

        /** Taken out by a caller, so it is no longer pending even though its run ends by cancellation. */
        var cancelled = false
    }

    private class PeriodicConfig(val interval: Duration, val request: InProcessWorkRequest)

    private inner class PeriodicWork(val name: String, initial: PeriodicConfig) {
        val config = MutableStateFlow(initial)
        var job: Job? = null

        fun start() {
            job = scope.launch {
                while (true) {
                    val current = config.value
                    val now = nowMillis()
                    val lastRun = persisted.lastRunAt(name)
                    val due = when {
                        // A first schedule runs at once, as WorkManager's do, and so does one the clock went back on
                        lastRun == null || lastRun > now -> now
                        else -> maxOf(now, lastRun + current.interval.inWholeMilliseconds)
                    }
                    nextRuns.update { it + (name to due) }
                    val changed = withTimeoutOrNull((due - now).milliseconds) { config.first { it !== current } }
                    if (changed != null) continue
                    if (current.request.requiresNetwork) network.state().first { it.isOnline }
                    val startedAt = nowMillis()
                    persisted.recordScheduledRun(name, startedAt)
                    nextRuns.update { it + (name to startedAt + current.interval.inWholeMilliseconds) }
                    runPasses(name, current.request)
                }
            }
        }
    }

    private companion object {
        // WorkManager's bounds
        val MIN_BACKOFF = 10.seconds
        val MAX_BACKOFF = 5.hours

        fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

        /** How long to wait after run [attempt] asked to be retried, counting runs from 0, as WorkManager does. */
        fun WorkBackoff.delayAfter(attempt: Int): Duration {
            val delay = if (exponential) {
                initialDelay * (1L shl attempt.coerceAtMost(30)).toDouble()
            } else {
                initialDelay *
                    (attempt + 1)
            }
            return delay.coerceIn(MIN_BACKOFF, MAX_BACKOFF)
        }
    }
}
