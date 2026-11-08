package mihon.desktop.application

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Starts the app again, for settings that only apply at start and from the crash screen. [request] asks the application
 * to quit as it does on Quit; once it has, [relaunchIfRequested] starts it again through [AppRelaunch].
 */
@Inject
@SingleIn(AppScope::class)
class AppRestart(private val relaunch: AppRelaunch) {
    private val requested = MutableStateFlow(false)

    val isRequested: StateFlow<Boolean> = requested.asStateFlow()

    fun request() {
        requested.value = true
    }

    fun relaunchIfRequested() {
        if (requested.value) relaunch.start()
    }
}
