package mihon.feature.appupdate.mpvaddon.ui

import androidx.compose.runtime.Composable
import mihon.presentation.kit.dialog.ConfirmDialog
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** After an add-on update in a process that already runs the previous add-on's libraries. */
@Composable
internal fun MpvAddonRestartDialog(onRestart: () -> Unit, onDismiss: () -> Unit) {
    ConfirmDialog(
        title = stringResource(MR.strings.mpv_addon_restart_title),
        text = stringResource(MR.strings.mpv_addon_restart_text),
        confirmLabel = stringResource(MR.strings.mpv_addon_restart),
        onConfirm = onRestart,
        onDismissRequest = onDismiss,
        dismissLabel = stringResource(MR.strings.action_not_now),
    )
}
