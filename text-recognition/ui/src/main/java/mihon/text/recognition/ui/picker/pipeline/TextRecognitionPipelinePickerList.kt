package mihon.text.recognition.ui.picker.pipeline

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.api.state.ModelArtifactState
import mihon.model.artifacts.ui.state.formatModelArtifactSize
import mihon.model.artifacts.ui.state.modelArtifactStateLabel
import mihon.text.recognition.api.pipeline.TextRecognitionPipelineSelection
import mihon.text.recognition.api.provider.TextRecognitionBuildAvailability
import mihon.text.recognition.ui.settings.TextRecognitionPipelineOption
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.i18n.stringResource

/** Recommended pipelines followed by every custom combination the catalog allows for one language. */
@Composable
fun TextRecognitionPipelinePickerList(
    options: List<TextRecognitionPipelineOption>,
    selected: TextRecognitionPipelineSelection?,
    onSelect: (TextRecognitionPipelineSelection) -> Unit,
    onDownload: (List<ModelArtifactDescriptor>) -> Unit,
    onCancelDownload: (List<ModelArtifactDescriptor>) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
) {
    val (presets, customs) = options.partition { it.selection is TextRecognitionPipelineSelection.Preset }
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (presets.isNotEmpty()) {
            item { HeadingItem(stringResource(MR.strings.text_recognition_settings_presets)) }
            items(presets, key = { it.selection.toString() }) { option ->
                PipelineCard(option, option.selection == selected, onSelect, onDownload, onCancelDownload)
            }
        }
        if (customs.isNotEmpty()) {
            item { HeadingItem(stringResource(MR.strings.text_recognition_settings_custom)) }
            items(customs, key = { it.selection.toString() }) { option ->
                PipelineCard(option, option.selection == selected, onSelect, onDownload, onCancelDownload)
            }
        }
    }
}

@Composable
private fun PipelineCard(
    option: TextRecognitionPipelineOption,
    selected: Boolean,
    onSelect: (TextRecognitionPipelineSelection) -> Unit,
    onDownload: (List<ModelArtifactDescriptor>) -> Unit,
    onCancelDownload: (List<ModelArtifactDescriptor>) -> Unit,
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                enabled = option.included,
                role = Role.RadioButton,
                onClick = { onSelect(option.selection) },
            ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = option.title ?: option.components.joinToString(" + ") { it.displayName },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    val secondary = option.description
                        ?: option.components.joinToString(" · ") { it.description }
                    Text(
                        text = secondary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                RadioButton(selected = selected, enabled = option.included, onClick = null)
            }
            val excludedBy = option.excludedBy
            when {
                excludedBy != null -> Text(
                    text = (excludedBy.buildAvailability as? TextRecognitionBuildAvailability.NotIncluded)?.reason
                        ?: excludedBy.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                option.models.isEmpty() -> Unit
                option.models.all { (_, state) -> state is ModelArtifactState.Installed } -> Text(
                    text = stringResource(MR.strings.text_recognition_settings_models_ready),
                    style = MaterialTheme.typography.bodySmall,
                )
                else -> option.models.forEach { (descriptor, state) ->
                    Text(
                        text = "${descriptor.displayName}: ${modelArtifactStateLabel(descriptor, state)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (excludedBy == null) {
                when {
                    option.downloadingModels.isNotEmpty() -> TextButton(
                        onClick = { onCancelDownload(option.downloadingModels) },
                    ) {
                        Text(stringResource(MR.strings.action_cancel))
                    }
                    option.missingModels.isNotEmpty() -> FilledTonalButton(onClick = {
                        onDownload(option.missingModels)
                    }) {
                        Text(
                            stringResource(
                                MR.strings.text_recognition_settings_download_models,
                                formatModelArtifactSize(option.missingModels.sumOf { it.sizeBytes }),
                            ),
                        )
                    }
                }
            }
        }
    }
}
