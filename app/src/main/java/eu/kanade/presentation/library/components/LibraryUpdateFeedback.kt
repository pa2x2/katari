package eu.kanade.presentation.library.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.tachiyomi.data.library.LibraryUpdateState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Close
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.progress.LinearProgressIndicator
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
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
    val context = LocalContext.current
    val progress = (state as? LibraryUpdateState.Running)?.progress
    val label = when (state) {
        is LibraryUpdateState.Running -> remember(state.progress, context) { state.progress.label(context) }
        is LibraryUpdateState.Waiting -> stringResource(MR.strings.library_update_waiting, state.runningProfileName)
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        container = SurfaceContainer.Default,
    ) {
        Row(
            modifier = Modifier.padding(start = KatariTheme.spacing.l, end = KatariTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            ) {
                Text(
                    text = label,
                    role = TextRole.Supporting,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (progress != null && progress.total > 0) {
                    LinearProgressIndicator(
                        progress = { progress.completed.toFloat() / progress.total },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            IconButton(
                icon = MaterialSymbols.Rounded.Close,
                contentDescription = stringResource(MR.strings.library_update_cancel),
                onClick = onCancel,
            )
        }
    }
}
