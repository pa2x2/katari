package mihon.entry.interactions.book.epub

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Dispatches the first value immediately, then the latest pending value at a bounded rate. */
internal class ThrottledLatestDispatcher<T>(
    private val scope: CoroutineScope,
    private val intervalMillis: Long,
    private val dispatch: (T) -> Unit,
) {
    private var pending: T? = null
    private var job: Job? = null

    fun preview(value: T) {
        pending = value
        if (job?.isActive == true) return
        job = scope.launch {
            while (true) {
                val next = pending ?: break
                pending = null
                dispatch(next)
                delay(intervalMillis)
            }
        }
    }

    fun finish(value: T) {
        pending = null
        job?.cancel()
        job = null
        dispatch(value)
    }

    fun cancel() {
        pending = null
        job?.cancel()
        job = null
    }
}
