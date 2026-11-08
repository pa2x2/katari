package eu.kanade.presentation.library.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import app.katari.core.platform.currentPlatformContext
import app.katari.presentation.kit.feedback.StatusStrip
import app.katari.presentation.kit.feedback.StatusStripTone
import eu.kanade.tachiyomi.data.library.label
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import tachiyomi.domain.library.update.model.LibraryUpdateState
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.time.Duration.Companion.seconds

/**
 * Pull-to-refresh state for starting a library update: set it once the update is started or queued, and it clears
 * when the update shows up, handing over to [LibraryUpdateStrip], or when the update never shows up.
 */
@Composable
internal fun rememberAwaitingUpdateState(updateState: LibraryUpdateState?): MutableState<Boolean> {
    val awaitingUpdate = remember { mutableStateOf(false) }
    val currentUpdateState by rememberUpdatedState(updateState)
    LaunchedEffect(awaitingUpdate.value) {
        if (!awaitingUpdate.value) return@LaunchedEffect
        withTimeoutOrNull(UpdateStartTimeout) {
            snapshotFlow { currentUpdateState }.first { it != null }
        }
        awaitingUpdate.value = false
    }
    return awaitingUpdate
}

private val UpdateStartTimeout = 10.seconds

/**
 * Shows this profile's library update while there is one, keeping its last state while it animates out.
 *
 * @param onCancel stops the update, or takes a waiting one out of the queue.
 */
@Composable
internal fun LibraryUpdateStrip(
    state: LibraryUpdateState?,
    onCancel: () -> Unit,
) {
    var lastState by remember { mutableStateOf(state) }
    LaunchedEffect(state) {
        if (state != null) lastState = state
    }
    AnimatedVisibility(visible = state != null) {
        (state ?: lastState)?.let {
            LibraryUpdateStripContent(state = it, onCancel = onCancel)
        }
    }
}

/** What a running update covers and how far it got, or whose update a waiting one waits for. */
@Composable
private fun LibraryUpdateStripContent(
    state: LibraryUpdateState,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = currentPlatformContext()
    val progress = (state as? LibraryUpdateState.Running)?.progress
    val label = when (state) {
        is LibraryUpdateState.Running -> remember(state.progress, context) { state.progress.label(context) }
        is LibraryUpdateState.Waiting -> stringResource(MR.strings.library_update_waiting, state.runningProfileName)
    }
    val fraction = progress?.takeIf { it.total > 0 }?.let { it.completed.toFloat() / it.total }
    StatusStrip(
        text = label,
        tone = StatusStripTone.Neutral,
        modifier = modifier,
        waiting = fraction == null,
        progress = fraction?.let { { it } },
        actionLabel = stringResource(MR.strings.action_cancel),
        onAction = onCancel,
    )
}
