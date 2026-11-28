package app.katari.backupconversion.issue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.dialog.Dialog
import app.katari.presentation.kit.dialog.DialogAction
import app.katari.presentation.kit.list.ToggleRow
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Before Report or Ask opens GitHub's form: Katari saves a file with what it found, with the backup in it when
 * [includeBackup] (D132); [asking] for Ask's form (D134).
 */
@Composable
internal fun BackupIssueDialog(
    asking: Boolean,
    includeBackup: Boolean,
    onIncludeBackupChange: (Boolean) -> Unit,
    onContinue: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(
            if (asking) MR.strings.backup_conversion_ask_title else MR.strings.backup_conversion_report_title,
        ),
        confirm = DialogAction(stringResource(MR.strings.backup_conversion_report_continue), onContinue),
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismissRequest),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m)) {
            Text(stringResource(MR.strings.backup_conversion_report_text), role = TextRole.Body)
            ToggleRow(
                title = stringResource(MR.strings.backup_conversion_report_include_backup),
                subtitle = stringResource(MR.strings.backup_conversion_report_include_backup_why),
                checked = includeBackup,
                onCheckedChange = onIncludeBackupChange,
            )
        }
    }
}
