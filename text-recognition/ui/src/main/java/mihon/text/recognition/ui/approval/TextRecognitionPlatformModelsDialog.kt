package mihon.text.recognition.ui.approval

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import mihon.model.artifacts.ui.state.formatModelArtifactSize
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(MR.strings.model_artifacts_approval_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(description)
                approximateSizeBytes?.let {
                    Text(stringResource(MR.strings.model_artifacts_approval_total, formatModelArtifactSize(it)))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onApprove) {
                Text(stringResource(MR.strings.action_download))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(MR.strings.action_cancel))
            }
        },
    )
}
