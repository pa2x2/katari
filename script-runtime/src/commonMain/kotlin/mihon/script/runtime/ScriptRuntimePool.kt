package mihon.script.runtime

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Keeps runtimes between calls, up to [maxPerKey] per key, so that one long call (a background refresh) doesn't make
 * another (a browse) wait. A runtime is closed after [idleClose] without a call, when a call stops it, or when its
 * key is [invalidate]d; a call that was using it keeps it until it returns.
 */
class ScriptRuntimePool(
    private val factory: ScriptRuntimeFactory,
    private val maxPerKey: Int = 2,
    private val idleClose: Duration = 2.minutes,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val slots = HashMap<Any, Slot>()

    private class Slot(permits: Int) {
        val permits = Semaphore(permits)
        val idle = ArrayDeque<Idle>()
        var generation = 0
    }

    private class Idle(val runtime: ScriptRuntime, val generation: Int, val closer: Job)

    /** Runs [block] on a runtime for [key], opening one from [spec] when none is idle. */
    suspend fun <T> use(key: Any, spec: () -> ScriptRuntimeSpec, block: suspend (ScriptRuntime) -> T): T {
        val slot = synchronized(slots) { slots.getOrPut(key) { Slot(maxPerKey) } }
        return slot.permits.withPermit {
            // invalidate empties the idle list, so whatever is in it belongs to the current generation
            val idle = synchronized(slots) { slot.idle.removeLastOrNull()?.also { it.closer.cancel() } }
            val generation = idle?.generation ?: synchronized(slots) { slot.generation }
            val runtime = idle?.runtime ?: factory.open(spec())
            try {
                block(runtime)
            } finally {
                withContext(NonCancellable) { release(slot, runtime, generation) }
            }
        }
    }

    /** Closes the idle runtimes of every key matching [predicate]; busy ones close when their call returns. */
    fun invalidate(predicate: (key: Any) -> Boolean) {
        val stale = synchronized(slots) {
            slots.filterKeys(predicate).values.flatMap { slot ->
                slot.generation++
                slot.idle.toList().also { slot.idle.clear() }
            }
        }
        stale.forEach {
            it.closer.cancel()
            it.runtime.close()
        }
    }

    private fun release(slot: Slot, runtime: ScriptRuntime, generation: Int) {
        val kept = synchronized(slots) {
            if (!runtime.isOpen || generation != slot.generation) return@synchronized false
            val closer = scope.launch {
                delay(idleClose)
                val expired = synchronized(slots) { slot.idle.removeAll { it.runtime === runtime } }
                if (expired) runtime.close()
            }
            slot.idle.addLast(Idle(runtime, generation, closer))
            true
        }
        if (!kept) runtime.close()
    }
}
