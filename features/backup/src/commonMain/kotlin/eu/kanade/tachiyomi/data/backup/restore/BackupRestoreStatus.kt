package eu.kanade.tachiyomi.data.backup.restore

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * How the last restore the user started is going, for setup and the Library to show while it runs and after (D120,
 * D142).
 * [BackupRestorer] keeps it beside the platform's [BackupRestoreReporter]; a library sync's restore isn't one the user
 * started, so it isn't shown here.
 */
@Inject
@SingleIn(AppScope::class)
class BackupRestoreStatus {
    /** Null until a restore starts while the app runs, and again once the person puts its end away. */
    val state: StateFlow<State?>
        field = MutableStateFlow<State?>(null)

    sealed interface State {
        /** [progress] of [total] parts restored: entries, categories, settings and stores. */
        data class Running(val progress: Int, val total: Int) : State

        data class Restored(val errorCount: Int) : State

        /** It failed or was cancelled before it finished. */
        data object Stopped : State
    }

    /** Puts away how the last restore ended; one that still runs stays. */
    fun dismiss() {
        state.update { if (it is State.Running) it else null }
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
