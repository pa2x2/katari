package app.katari.tools.extensioncompatibility.run

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

// Calls run outside the caller's scope, so a timeout doesn't wait for them to stop
private val calls = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/**
 * [block]'s result or failure, or `null` when it takes longer than [timeout]. An extension blocking in a call that
 * ignores cancellation, such as OkHttp's `execute`, is left to finish on its own instead of holding the run up.
 */
internal suspend fun <T> withAbandoningTimeout(timeout: Duration, block: suspend () -> T): Result<T>? {
    val call = calls.async { block() }
    val result = withTimeoutOrNull(timeout) {
        try {
            Result.success(call.await())
        } catch (e: Throwable) {
            // A cancellation of the extension's own, such as its own timeout, is its failure; the run's isn't
            currentCoroutineContext().ensureActive()
            Result.failure(e)
        }
    }
    if (result == null) call.cancel()
    return result
}
