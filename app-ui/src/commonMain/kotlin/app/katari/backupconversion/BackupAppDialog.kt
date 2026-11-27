package app.katari.backupconversion

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import app.katari.presentation.kit.dialog.Dialog
import app.katari.presentation.kit.dialog.DialogAction
import app.katari.presentation.kit.list.ChoiceRow
import eu.kanade.tachiyomi.data.backup.conversion.BackupApp
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Every app the tool reads, forks as their own choices, to say which wrote the file (D015). */
@Composable
internal fun BackupAppDialog(
    apps: List<BackupApp>,
    selected: BackupApp?,
    detected: BackupApp?,
    onPick: (BackupApp) -> Unit,
    onDismissRequest: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(MR.strings.backup_conversion_which_app),
        confirm = null,
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismissRequest),
    ) {
        Column {
            apps.forEach { app ->
                ChoiceRow(
                    title = app.name,
                    subtitle = stringResource(MR.strings.backup_conversion_detected).takeIf { app == detected },
                    selected = app == selected,
                    onClick = { onPick(app) },
                )
            }
        }
    }
}
