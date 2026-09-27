package eu.kanade.presentation.more.settings.screen.textrecognition.pipeline

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import eu.kanade.presentation.components.AppBar
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.api.download.ModelArtifactDownloadApproval
import mihon.model.artifacts.api.state.ModelArtifactState
import mihon.model.artifacts.ui.approval.ModelArtifactDownloadApprovalDialog
import mihon.model.artifacts.ui.state.formatModelArtifactSize
import mihon.model.artifacts.ui.state.modelArtifactStateLabel
import mihon.text.recognition.api.pipeline.TextRecognitionPipelineSelection
import mihon.translation.ui.picker.language.displayName
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen

@Composable
internal fun TextRecognitionPipelinePickerContent(
    state: TextRecognitionPipelinePickerState?,
    onSelect: (TextRecognitionPipelineSelection?) -> Unit,
    onDownload: (List<ModelArtifactDownloadApproval>) -> Unit,
    onCancelDownload: (List<ModelArtifactDescriptor>) -> Unit,
    onBack: () -> Unit,
) {
    var pendingApproval by remember { mutableStateOf<List<ModelArtifactDescriptor>?>(null) }
    Scaffold(
        topBar = {
            AppBar(
                title = state?.language?.displayName() ?: stringResource(MR.strings.text_recognition_title),
                navigateUp = onBack,
                scrollBehavior = it,
            )
        },
    ) { contentPadding ->
        if (state == null) {
            LoadingScreen(Modifier.padding(contentPadding))
            return@Scaffold
        }
        LazyColumn(contentPadding = contentPadding) {
            item {
                val default = state.defaultSelection?.let { default ->
                    (state.presets + state.customPipelines).firstOrNull { it.selection == default }?.title
                }
                SelectableRow(
                    selected = state.explicitSelection == null,
                    enabled = true,
                    title = stringResource(MR.strings.text_recognition_settings_use_default),
                    supporting =
                    default?.let { stringResource(MR.strings.text_recognition_settings_default_pipeline, it) }
                        ?: stringResource(MR.strings.text_recognition_settings_no_pipeline),
                    onClick = { onSelect(null) },
                )
            }
            if (state.presets.isNotEmpty()) {
                item { HeadingItem(stringResource(MR.strings.text_recognition_settings_presets)) }
                items(state.presets, key = { it.selection.toString() }) { option ->
                    OptionRow(
                        option = option,
                        selected = state.explicitSelection == option.selection,
                        onSelect = onSelect,
                        onDownload = { pendingApproval = option.missingModels },
                        onCancelDownload = onCancelDownload,
                    )
                }
            }
            if (state.customPipelines.isNotEmpty()) {
                item { HeadingItem(stringResource(MR.strings.text_recognition_settings_custom)) }
                items(state.customPipelines, key = { it.selection.toString() }) { option ->
                    OptionRow(
                        option = option,
                        selected = state.explicitSelection == option.selection,
                        onSelect = onSelect,
                        onDownload = { pendingApproval = option.missingModels },
                        onCancelDownload = onCancelDownload,
                    )
                }
            }
        }
    }
    pendingApproval?.let { artifacts ->
        ModelArtifactDownloadApprovalDialog(
            artifacts = artifacts,
            onApprove = { approvals ->
                pendingApproval = null
                onDownload(approvals)
            },
            onDismiss = { pendingApproval = null },
        )
    }
}

@Composable
private fun OptionRow(
    option: TextRecognitionPipelineOption,
    selected: Boolean,
    onSelect: (TextRecognitionPipelineSelection) -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: (List<ModelArtifactDescriptor>) -> Unit,
) {
    val exclusion = option.exclusion
    val downloading = option.models.filter { (_, state) -> state is ModelArtifactState.Downloading }.map { it.first }
    SelectableRow(
        selected = selected,
        enabled = exclusion == null,
        title = option.title,
        supporting = null,
        onClick = { onSelect(option.selection) },
        details = {
            option.description?.let { Text(it) }
            when {
                exclusion != null -> Text(
                    text = exclusion.reason,
                    color = MaterialTheme.colorScheme.error,
                )
                option.models.isEmpty() -> Unit
                option.models.all { (_, state) -> state is ModelArtifactState.Installed } ->
                    Text(stringResource(MR.strings.text_recognition_settings_models_ready))
                else -> option.models.forEach { (descriptor, state) ->
                    Text("${descriptor.displayName}: ${modelArtifactStateLabel(descriptor, state)}")
                }
            }
        },
        trailing = {
            when {
                exclusion != null -> Unit
                downloading.isNotEmpty() -> IconButton(onClick = { onCancelDownload(downloading) }) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(MR.strings.action_cancel))
                }
                option.missingModels.isNotEmpty() -> IconButton(onClick = onDownload) {
                    Icon(
                        Icons.Outlined.Download,
                        contentDescription = stringResource(
                            MR.strings.text_recognition_settings_models_needed,
                            formatModelArtifactSize(option.missingModels.sumOf { it.sizeBytes }),
                        ),
                    )
                }
            }
        },
    )
}

@Composable
private fun SelectableRow(
    selected: Boolean,
    enabled: Boolean,
    title: String,
    supporting: String?,
    onClick: () -> Unit,
    details: @Composable () -> Unit = { supporting?.let { Text(it) } },
    trailing: @Composable () -> Unit = {},
) {
    ListItem(
        modifier = Modifier.selectable(
            selected = selected,
            enabled = enabled,
            role = Role.RadioButton,
            onClick = onClick,
        ),
        leadingContent = { RadioButton(selected = selected, onClick = null, enabled = enabled) },
        supportingContent = { Column { details() } },
        trailingContent = trailing,
        content = { Text(title) },
    )
}
