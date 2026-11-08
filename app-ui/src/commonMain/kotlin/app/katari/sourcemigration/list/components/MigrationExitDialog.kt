package app.katari.sourcemigration.list.components

import androidx.compose.runtime.Composable
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.StopCircle
import app.katari.presentation.kit.dialog.ConfirmDialog
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun MigrationExitDialog(
    onDismissRequest: () -> Unit,
    exitMigration: () -> Unit,
) {
    ConfirmDialog(
        title = stringResource(MR.strings.migrationListScreen_exitDialogTitle),
        text = null,
        confirmLabel = stringResource(MR.strings.migrationListScreen_exitDialog_stopLabel),
        onConfirm = exitMigration,
        onDismissRequest = onDismissRequest,
        destructive = true,
        icon = MaterialSymbols.Rounded.StopCircle,
        dismissLabel = stringResource(MR.strings.migrationListScreen_exitDialog_cancelLabel),
    )
}
