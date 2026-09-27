package mihon.model.artifacts.ui.approval

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.api.descriptor.ModelArtifactHosting
import mihon.model.artifacts.api.download.ModelArtifactDownloadApproval
import mihon.model.artifacts.ui.state.formatModelArtifactSize
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Discloses size, license, and origin of [artifacts] and turns the user's confirmation into download approvals.
 * This is the only way hosts should obtain a [ModelArtifactDownloadApproval].
 */
@Composable
fun ModelArtifactDownloadApprovalDialog(
    artifacts: List<ModelArtifactDescriptor>,
    onApprove: (List<ModelArtifactDownloadApproval>) -> Unit,
    onDismiss: () -> Unit,
) {
    var allowMetered by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(MR.strings.model_artifacts_approval_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(MR.strings.model_artifacts_approval_message))
                artifacts.forEach { artifact -> ArtifactDisclosure(artifact) }
                Text(
                    text = stringResource(
                        MR.strings.model_artifacts_approval_total,
                        formatModelArtifactSize(artifacts.sumOf(ModelArtifactDescriptor::sizeBytes)),
                    ),
                    style = MaterialTheme.typography.titleSmall,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = allowMetered, onCheckedChange = { allowMetered = it })
                    Text(stringResource(MR.strings.model_artifacts_approval_allow_metered))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onApprove(artifacts.map { ModelArtifactDownloadApproval(it, allowMeteredNetwork = allowMetered) })
                },
            ) {
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

@Composable
private fun ArtifactDisclosure(artifact: ModelArtifactDescriptor) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "${artifact.displayName} · ${formatModelArtifactSize(artifact.sizeBytes)}",
            style = MaterialTheme.typography.bodyLarge,
        )
        val secondary = MaterialTheme.typography.bodySmall
        Text(stringResource(MR.strings.model_artifacts_approval_license, artifact.license.name), style = secondary)
        val hosting = artifact.hosting
        Text(
            text = when (hosting) {
                is ModelArtifactHosting.Upstream ->
                    stringResource(MR.strings.model_artifacts_approval_source, hosting.sourceUrl.location())
                is ModelArtifactHosting.Project ->
                    stringResource(MR.strings.model_artifacts_approval_project_hosted, hosting.upstreamUrl.location())
            },
            style = secondary,
        )
    }
}

/** Host and path of a publication URL, without scheme or query. */
private fun String.location(): String = Uri.parse(this).let { uri -> uri.host?.let { it + uri.path.orEmpty() } } ?: this
