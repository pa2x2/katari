package mihon.text.recognition.ui.picker.pipeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.api.state.ModelArtifactState
import mihon.model.artifacts.ui.state.formatModelArtifactSize
import mihon.model.artifacts.ui.state.modelArtifactStateLabel
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.ChoiceRow
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import mihon.text.recognition.api.provider.TextRecognitionBuildAvailability
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * A pipeline as a choice, with why this build cannot run it or the state of the models it needs and the action that
 * downloads them or cancels their download.
 */
@Composable
internal fun TextRecognitionPipelineRow(
    option: TextRecognitionPipelineOption,
    title: String,
    description: String,
    selected: Boolean,
    onSelect: () -> Unit,
    onDownload: (List<ModelArtifactDescriptor>) -> Unit,
    onCancelDownload: (List<ModelArtifactDescriptor>) -> Unit,
) {
    ChoiceRow(
        title = title,
        selected = selected,
        onClick = onSelect,
        subtitle = description,
        enabled = option.included,
        below = if (option.excludedBy != null || option.models.isNotEmpty()) {
            {
                Column(
                    modifier = Modifier.padding(top = KatariTheme.spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
                ) {
                    PipelineState(option)
                    PipelineModelsAction(option, onDownload, onCancelDownload)
                }
            }
        } else {
            null
        },
    )
}

@Composable
private fun PipelineState(option: TextRecognitionPipelineOption) {
    val excludedBy = option.excludedBy
    when {
        excludedBy != null -> Text(
            text = (excludedBy.buildAvailability as? TextRecognitionBuildAvailability.NotIncluded)?.reason
                ?: excludedBy.name,
            role = TextRole.Body,
            tone = ContentTone.Error,
        )
        option.models.isEmpty() -> Unit
        option.models.all { (_, state) -> state is ModelArtifactState.Installed } -> Text(
            text = stringResource(MR.strings.text_recognition_settings_models_ready),
            role = TextRole.Supporting,
        )
        else -> option.models.forEach { (descriptor, state) ->
            Text(
                text = "${descriptor.displayName}: ${modelArtifactStateLabel(descriptor, state)}",
                role = TextRole.Supporting,
            )
        }
    }
}

@Composable
private fun PipelineModelsAction(
    option: TextRecognitionPipelineOption,
    onDownload: (List<ModelArtifactDescriptor>) -> Unit,
    onCancelDownload: (List<ModelArtifactDescriptor>) -> Unit,
) {
    if (option.excludedBy != null) return
    when {
        option.downloadingModels.isNotEmpty() -> Button(
            label = stringResource(MR.strings.action_cancel),
            onClick = { onCancelDownload(option.downloadingModels) },
            style = ButtonStyle.Text,
        )
        option.missingModels.isNotEmpty() -> Button(
            label = stringResource(
                MR.strings.text_recognition_settings_download_models,
                formatModelArtifactSize(option.missingModels.sumOf { it.sizeBytes }),
            ),
            onClick = { onDownload(option.missingModels) },
            style = ButtonStyle.Tonal,
        )
    }
}
