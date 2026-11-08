package app.katari.ui.host

import app.katari.core.metro.LaunchTask
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.ui.main.LaunchCleanup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * What each launch of the app does once its profile is open: the launch clean-up, then every contributed [LaunchTask]
 * side by side. [run] returns once the clean-up has started and every task has returned; [scope] is what the clean-up
 * and the tasks start their background work in.
 */
@Inject
class AppLaunch(
    private val cleanup: LaunchCleanup,
    private val tasks: Set<LaunchTask>,
) {
    suspend fun run(scope: CoroutineScope) {
        cleanup.run(scope)
        coroutineScope {
            tasks.forEach { task -> launch { task.run(scope) } }
        }
    }
}
