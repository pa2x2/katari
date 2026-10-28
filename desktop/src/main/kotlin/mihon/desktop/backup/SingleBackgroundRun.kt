package mihon.desktop.backup

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Work that runs in [scope] one run at a time: starting it while it runs keeps the current run, as Android's unique
 * work does.
 */
internal class SingleBackgroundRun(private val scope: CoroutineScope) {
    private var job: Job? = null

    val isActive: Boolean
        get() = synchronized(this) { job?.isActive == true }

    fun start(block: suspend CoroutineScope.() -> Unit) {
        synchronized(this) {
            if (job?.isActive == true) return
            job = scope.launch(block = block)
        }
    }

    fun cancel() {
        synchronized(this) { job?.cancel() }
    }
}
