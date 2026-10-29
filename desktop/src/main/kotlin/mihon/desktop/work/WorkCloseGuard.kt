package mihon.desktop.work

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mihon.presentation.kit.dialog.ConfirmDialog
import tachiyomi.i18n.MR
import tachiyomi.i18n.action_quit
import tachiyomi.i18n.action_wait
import tachiyomi.i18n.quit_while_working_text
import tachiyomi.i18n.quit_while_working_title
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Asks before the app quits while background work is running a pass, which quitting would cut short; the work starts
 * again at the next launch. Android's work outlives the app's screens instead.
 */
@Inject
@SingleIn(AppScope::class)
class WorkCloseGuard(private val work: InProcessWork) {
    private val asking = MutableStateFlow(false)

    /** Whether the question is showing. */
    val isAsking: StateFlow<Boolean> = asking.asStateFlow()

    /** Quits through [quit] at once while no work runs, and asks first otherwise. */
    fun requestQuit(quit: () -> Unit) {
        if (work.isWorking) asking.value = true else quit()
    }

    fun dismiss() {
        asking.value = false
    }
}

/** The [WorkCloseGuard]'s question: quit now through [quit], or wait and keep the app open. */
@Composable
fun WorkCloseGuardDialog(guard: WorkCloseGuard, quit: () -> Unit) {
    val asking by guard.isAsking.collectAsState()
    if (!asking) return
    ConfirmDialog(
        title = stringResource(MR.strings.quit_while_working_title),
        text = stringResource(MR.strings.quit_while_working_text),
        confirmLabel = stringResource(MR.strings.action_quit),
        onConfirm = {
            guard.dismiss()
            quit()
        },
        onDismissRequest = guard::dismiss,
        destructive = true,
        dismissLabel = stringResource(MR.strings.action_wait),
    )
}
