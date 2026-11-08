package tachiyomi.core.common.util.lang

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Keeps an observation of live data, such as a screen's database query, going through a failure. Catching it would
 * end the flow, and the screen would show what it last had until the app restarted, however the data changed.
 *
 * Each failure is logged as [owner]'s and handed to [onFailure], which may emit what to show meanwhile or tell the
 * person; the flow then subscribes again after a pause, longer for each failure in a row up to [MAX_PAUSE], so a
 * fault that lasts is retried without spinning. A value arriving ends the row. Cancellation still ends the flow.
 */
fun <T> Flow<T>.resubscribeOnFailure(
    owner: Any,
    onFailure: suspend FlowCollector<T>.(Throwable) -> Unit = {},
): Flow<T> = flow {
    var inARow = 0
    emitAll(
        onEach { inARow = 0 }.retryWhen { cause, _ ->
            if (cause is CancellationException) return@retryWhen false
            owner.logcat(LogPriority.ERROR, cause) { "Observation failed; subscribing again" }
            onFailure(cause)
            delay(pauseAfter(inARow++))
            true
        },
    )
}

private fun pauseAfter(failuresBefore: Int): Duration =
    (FIRST_PAUSE * (1 shl failuresBefore.coerceAtMost(MAX_DOUBLINGS))).coerceAtMost(MAX_PAUSE)

private val FIRST_PAUSE = 500.milliseconds
private const val MAX_DOUBLINGS = 6
private val MAX_PAUSE = 30.seconds
