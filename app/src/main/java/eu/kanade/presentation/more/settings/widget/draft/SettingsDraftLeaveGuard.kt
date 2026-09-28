package eu.kanade.presentation.more.settings.widget.draft

import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Asks whether to store unsaved settings edits before the screen is left by the back gesture or by the returned action,
 * which the screen's up button must use. [onLeave] is null when the screen cannot leave by itself; the edits are then
 * stored or dropped and the screen stays.
 */
@Composable
fun rememberSettingsDraftLeaveGuard(
    hasUnsavedChanges: Boolean,
    saveEnabled: Boolean,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onLeave: (() -> Unit)?,
): () -> Unit {
    var confirming by rememberSaveable { mutableStateOf(false) }
    val unsaved by rememberUpdatedState(hasUnsavedChanges)
    val leave by rememberUpdatedState(onLeave)

    BackHandler(enabled = hasUnsavedChanges) { confirming = true }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(stringResource(MR.strings.settings_draft_leave_title)) },
            text = { Text(stringResource(MR.strings.settings_draft_leave_message)) },
            confirmButton = {
                TextButton(
                    enabled = saveEnabled,
                    onClick = {
                        confirming = false
                        onSave()
                        leave?.invoke()
                    },
                ) {
                    Text(stringResource(MR.strings.action_save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        confirming = false
                        onDiscard()
                        leave?.invoke()
                    },
                ) {
                    Text(stringResource(MR.strings.action_discard))
                }
                TextButton(onClick = { confirming = false }) {
                    Text(stringResource(MR.strings.settings_draft_keep_editing))
                }
            },
        )
    }

    return { if (unsaved) confirming = true else leave?.invoke() }
}
