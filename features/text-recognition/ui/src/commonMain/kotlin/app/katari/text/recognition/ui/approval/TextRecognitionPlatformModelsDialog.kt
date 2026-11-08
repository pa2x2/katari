package app.katari.text.recognition.ui.approval

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import app.katari.model.artifacts.ui.state.formatModelArtifactSize
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.dialog.Dialog
import app.katari.presentation.kit.dialog.DialogAction
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Asks before the platform downloads recognition data, disclosing what an engine reported about it. Nothing is
 * downloaded unless the user confirms.
 */
@Composable
fun TextRecognitionPlatformModelsDialog(
    description: String,
    approximateSizeBytes: Long?,
    onApprove: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        title = stringResource(MR.strings.model_artifacts_approval_title),
        confirm = DialogAction(stringResource(MR.strings.action_download), onApprove),
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismiss),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m)) {
            Text(description, role = TextRole.Body)
            approximateSizeBytes?.let {
                Text(
                    text = stringResource(MR.strings.model_artifacts_approval_total, formatModelArtifactSize(it)),
                    role = TextRole.Body,
                )
            }
        }
    }
}
