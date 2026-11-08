package app.katari.sync.ui.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import app.katari.sync.client.status.SyncStatus
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Asks before this device leaves its account (D030). While changes made here wait to be sent, it says how many and
 * that the other devices won't get them, and offers Sync now to send them first (D003). The count follows the status,
 * so the warning goes once they are sent; Disconnect still leaves at once.
 */
@Composable
internal fun DisconnectDialog(
    status: SyncStatus?,
    onSyncNow: () -> Unit,
    onDisconnect: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val link = status?.link as? SyncStatus.Link.Linked
    val server = link?.server?.host.orEmpty()
    val unsent = status?.unsent?.toInt() ?: 0
    // Sync now does nothing while syncing is stopped, such as until this device signs in again
    val canSend = unsent > 0 && link?.stopped == null
    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(MR.strings.sync_disconnect_title),
        confirm = DialogAction(stringResource(MR.strings.sync_action_disconnect), onDisconnect, destructive = true),
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismissRequest),
        neutral = DialogAction(
            label = stringResource(MR.strings.sync_action_sync_now),
            onClick = onSyncNow,
            enabled = status?.activity !is SyncStatus.Activity.Syncing,
        ).takeIf { canSend },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m)) {
            if (unsent > 0) {
                Text(
                    pluralStringResource(MR.plurals.sync_disconnect_unsent, unsent, unsent, server),
                    tone = ContentTone.Warning,
                )
            }
            Text(stringResource(MR.strings.sync_disconnect_text, server))
        }
    }
}
