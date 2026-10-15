package mihon.model.artifacts.ui.approval

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.api.descriptor.ModelArtifactHosting
import mihon.model.artifacts.api.download.ModelArtifactDownloadApproval
import mihon.model.artifacts.ui.state.formatModelArtifactSize
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.control.Checkbox
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
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
    Dialog(
        onDismissRequest = onDismiss,
        title = stringResource(MR.strings.model_artifacts_approval_title),
        confirm = DialogAction(
            label = stringResource(MR.strings.action_download),
            onClick = {
                onApprove(artifacts.map { ModelArtifactDownloadApproval(it, allowMeteredNetwork = allowMetered) })
            },
        ),
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismiss),
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
        ) {
            Text(stringResource(MR.strings.model_artifacts_approval_message), role = TextRole.Body)
            artifacts.forEach { artifact -> ArtifactDisclosure(artifact) }
            Text(
                text = stringResource(
                    MR.strings.model_artifacts_approval_total,
                    formatModelArtifactSize(artifacts.sumOf(ModelArtifactDescriptor::sizeBytes)),
                ),
                role = TextRole.SectionTitle,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = allowMetered, onCheckedChange = { allowMetered = it })
                Text(stringResource(MR.strings.model_artifacts_approval_allow_metered), role = TextRole.Body)
            }
        }
    }
}

@Composable
private fun ArtifactDisclosure(artifact: ModelArtifactDescriptor) {
    Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xxs)) {
        Text(
            text = "${artifact.displayName} · ${formatModelArtifactSize(artifact.sizeBytes)}",
            role = TextRole.ItemTitle,
        )
        Text(
            text = stringResource(MR.strings.model_artifacts_approval_license, artifact.license.name),
            role = TextRole.Supporting,
        )
        val hosting = artifact.hosting
        Text(
            text = when (hosting) {
                is ModelArtifactHosting.Upstream ->
                    stringResource(MR.strings.model_artifacts_approval_source, hosting.sourceUrl.location())
                is ModelArtifactHosting.Project ->
                    stringResource(MR.strings.model_artifacts_approval_project_hosted, hosting.upstreamUrl.location())
            },
            role = TextRole.Supporting,
        )
    }
}

/** Host and path of a publication URL, without scheme or query. */
private fun String.location(): String = Uri.parse(this).let { uri -> uri.host?.let { it + uri.path.orEmpty() } } ?: this
