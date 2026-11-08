package eu.kanade.presentation.more.settings.screen.advanced

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.DeleteSweep
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.dialog.Dialog
import app.katari.presentation.kit.dialog.DialogAction
import app.katari.presentation.kit.list.ToggleRow
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Confirms removing the selected sources' entries that are not in the library, optionally keeping read ones. */
@Composable
internal fun ClearDatabaseConfirmDialog(
    onConfirm: (keepReadEntries: Boolean) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var keepReadEntries by remember { mutableStateOf(true) }
    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(MR.strings.are_you_sure),
        icon = MaterialSymbols.Rounded.DeleteSweep,
        confirm = DialogAction(
            label = stringResource(MR.strings.action_clear),
            onClick = { onConfirm(keepReadEntries) },
            destructive = true,
        ),
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onClick = onDismissRequest),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s)) {
            Text(stringResource(MR.strings.clear_database_text))
            ToggleRow(
                title = stringResource(MR.strings.clear_db_exclude_read),
                checked = keepReadEntries,
                onCheckedChange = { keepReadEntries = it },
            )
            if (!keepReadEntries) {
                Text(
                    text = stringResource(MR.strings.clear_database_history_warning),
                    role = TextRole.Supporting,
                    tone = ContentTone.Error,
                )
            }
        }
    }
}
