package eu.kanade.presentation.more.settings.widget.draft

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
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
        Dialog(
            onDismissRequest = { confirming = false },
            title = stringResource(MR.strings.settings_draft_leave_title),
            confirm = DialogAction(
                label = stringResource(MR.strings.action_save),
                enabled = saveEnabled,
                onClick = {
                    confirming = false
                    onSave()
                    leave?.invoke()
                },
            ),
            neutral = DialogAction(
                label = stringResource(MR.strings.action_discard),
                onClick = {
                    confirming = false
                    onDiscard()
                    leave?.invoke()
                },
            ),
            dismiss = DialogAction(
                label = stringResource(MR.strings.settings_draft_keep_editing),
                onClick = { confirming = false },
            ),
        ) {
            Text(stringResource(MR.strings.settings_draft_leave_message))
        }
    }

    return { if (unsaved) confirming = true else leave?.invoke() }
}
