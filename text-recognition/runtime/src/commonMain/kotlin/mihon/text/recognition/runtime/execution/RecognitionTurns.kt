package mihon.text.recognition.runtime.execution

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import mihon.text.recognition.api.request.TextRecognitionPriority

/**
 * Lets one recognition run at a time. When the running one finishes, the longest-waiting
 * [TextRecognitionPriority.Interactive] request goes next, and a [TextRecognitionPriority.Background] one only when no
 * interactive request waits. A running request is never interrupted.
 */
internal class RecognitionTurns {
    private val lock = Any()
    private var taken = false

    /** Waiting turns per lane, in the order lanes are served. */
    private val waiting = TextRecognitionPriority.entries.associateWith { ArrayDeque<CompletableDeferred<Unit>>() }

    suspend fun <T> withTurn(priority: TextRecognitionPriority, block: suspend () -> T): T {
        acquire(priority)
        try {
            return block()
        } finally {
            release()
        }
    }

    private suspend fun acquire(priority: TextRecognitionPriority) {
        val turn = synchronized(lock) {
            if (!taken) {
                taken = true
                return
            }
            CompletableDeferred<Unit>().also { waiting.getValue(priority).addLast(it) }
        }
        try {
            turn.await()
        } catch (error: CancellationException) {
            // Cancelled right after being handed the turn: pass it on instead of leaving it held.
            val granted = synchronized(lock) { !waiting.getValue(priority).remove(turn) }
            if (granted) release()
            throw error
        }
    }

    private fun release() {
        synchronized(lock) {
            val next = waiting.values.firstNotNullOfOrNull { it.removeFirstOrNull() }
            if (next == null) taken = false else next.complete(Unit)
        }
    }
}
