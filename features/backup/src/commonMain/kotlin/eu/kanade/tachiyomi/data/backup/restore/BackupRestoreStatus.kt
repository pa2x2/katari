package eu.kanade.tachiyomi.data.backup.restore

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * How the last restore the user started is going, for screens such as setup's to show while it runs and after (D120).
 * [BackupRestorer] keeps it beside the platform's [BackupRestoreReporter]; a library sync's restore isn't one the user
 * started, so it isn't shown here.
 */
@Inject
@SingleIn(AppScope::class)
class BackupRestoreStatus {
    /** Null until a restore starts while the app runs. */
    val state: StateFlow<State?>
        field = MutableStateFlow<State?>(null)

    sealed interface State {
        /** [progress] of [total] parts restored: entries, categories, settings and stores. */
        data class Running(val progress: Int, val total: Int) : State

        data class Restored(val errorCount: Int) : State

        /** It failed or was cancelled before it finished. */
        data object Stopped : State
    }

    internal fun started() {
        state.value = State.Running(progress = 0, total = 0)
    }

    internal fun stopped() {
        if (state.value is State.Running) state.value = State.Stopped
    }

    internal fun progressed(progress: Int, total: Int) {
        state.value = State.Running(progress, total)
    }

    internal fun restored(errorCount: Int) {
        state.value = State.Restored(errorCount)
    }
}
