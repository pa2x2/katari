package mihon.presentation.kit.feedback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/**
 * The two snackbar queues of one screen or sheet: [own], the snackbars the screen makes itself, and [messages], its
 * [ScreenMessages]. Each queue keeps its own order, and one snackbar of the two shows at a time ([shown]):
 * - a snackbar of the screen's own that closes on its own (Short or Long) shows first: a message waits behind it, and
 *   one arriving while a message shows takes its place until it closes, the message then showing for its whole time;
 * - a message shows next;
 * - a never-ending snackbar of the screen's own (Indefinite, which an action with no duration also gets) steps aside
 *   for a message and comes back after it. It stays its queue's current snackbar meanwhile, so it comes back with its
 *   action and the screen's `showSnackbar` doesn't return in between.
 */
internal class ScreenSnackbars(
    private val own: SnackbarHostState?,
    private val messages: SnackbarHostState?,
) {
    /**
     * The queue whose current snackbar is drawn. When neither has one, it is still a queue, so the host stays in place
     * and the last snackbar fades out; null only without any queue.
     */
    val shown: SnackbarHostState?
        get() {
            val ownSnackbar = own?.currentSnackbarData
            return when {
                ownSnackbar != null && ownSnackbar.visuals.duration != SnackbarDuration.Indefinite -> own
                messages?.currentSnackbarData != null -> messages
                ownSnackbar != null -> own
                else -> own ?: messages
            }
        }

    val isShowing: Boolean
        get() = shown?.currentSnackbarData != null
}

/**
 * Whether the scaffold around shows a snackbar now. A [mihon.presentation.kit.chip.FloatingNote] beside the floating
 * action steps aside meanwhile, so one note at a time floats above the action.
 */
internal val LocalSnackbarShowing = staticCompositionLocalOf<() -> Boolean> { { false } }

/**
 * Draws [own] and [messages] in one place, one snackbar at a time as [ScreenSnackbars] orders them. The kit's
 * [SnackbarHost] runs a snackbar's timeout only while it is drawn, so a message waiting behind a screen's snackbar keeps
 * its whole duration, and a snackbar that comes back restarts its own.
 */
@Composable
internal fun ScreenSnackbarHost(
    own: SnackbarHostState?,
    messages: SnackbarHostState?,
    modifier: Modifier = Modifier,
) {
    ScreenSnackbarHost(remember(own, messages) { ScreenSnackbars(own, messages) }, modifier)
}

@Composable
internal fun ScreenSnackbarHost(snackbars: ScreenSnackbars, modifier: Modifier = Modifier) {
    snackbars.shown?.let { SnackbarHost(it, modifier) }
}
