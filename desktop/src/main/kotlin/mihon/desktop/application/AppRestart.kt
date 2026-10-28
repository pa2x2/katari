package mihon.desktop.application

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.IOException
import kotlin.jvm.optionals.getOrNull

/**
 * Starts the app again, for settings that only apply at start. [request] asks the application to quit as it does on
 * Quit; once it has, [relaunchIfRequested] starts a new process with this one's command line and Java options, so the
 * app comes back the way it was started, from a packaged launcher or the build tool alike.
 */
@Inject
@SingleIn(AppScope::class)
class AppRestart {
    private val requested = MutableStateFlow(false)

    val isRequested: StateFlow<Boolean> = requested.asStateFlow()

    fun request() {
        requested.value = true
    }

    fun relaunchIfRequested() {
        if (!requested.value) return
        val process = ProcessHandle.current().info()
        val command = process.command().getOrNull()
        if (command == null) {
            logcat(LogPriority.ERROR) { "Can't restart: the system doesn't tell this process's command" }
            return
        }
        try {
            ProcessBuilder(listOf(command) + process.arguments().getOrNull().orEmpty()).inheritIO().start()
        } catch (e: IOException) {
            logcat(LogPriority.ERROR, e) { "Can't restart" }
        }
    }
}
