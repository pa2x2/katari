package mihon.script.runtime.host

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

/**
 * The host work of one call. Async host functions run here rather than in the engine's own job so that they can be
 * cancelled when the call has its result: the engine would otherwise hold the result until every pending request
 * and timer finished.
 */
internal class HostCall(hostContext: CoroutineContext) {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + hostContext + job)

    @Volatile
    private var lastProgress = System.nanoTime()

    val idleFor: Duration
        get() = (System.nanoTime() - lastProgress).nanoseconds

    /** Cancels whatever host work is still running; the script sees it as [CALL_ENDED]. */
    fun end() = job.cancel()

    suspend fun run(block: suspend () -> Any?): Any? {
        lastProgress = System.nanoTime()
        try {
            val work = scope.async { block() }
            return try {
                work.await()
            } catch (e: CancellationException) {
                // The engine cancelling this binding (the call was cancelled or closed) propagates; only the call's
                // own end turns into the marker
                currentCoroutineContext().ensureActive()
                if (job.isActive) throw e
                CALL_ENDED
            }
        } finally {
            lastProgress = System.nanoTime()
        }
    }

    companion object {
        /** What an async host function gives a script after its call ended; `host.js` never settles on it. */
        const val CALL_ENDED = "\u0000script-call-ended"
    }
}
