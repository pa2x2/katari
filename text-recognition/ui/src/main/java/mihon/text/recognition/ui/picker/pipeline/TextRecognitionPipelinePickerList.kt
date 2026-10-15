package mihon.text.recognition.ui.picker.pipeline

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.SectionHeader
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import mihon.text.recognition.api.pipeline.TextRecognitionPipelineSelection
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The automatic pipeline, then recommended pipelines, then combinations no preset offers, for one language. A `null`
 * selection is the automatic pipeline.
 */
@Composable
fun TextRecognitionPipelinePickerList(
    choices: TextRecognitionPipelineChoices,
    selected: TextRecognitionPipelineSelection?,
    onSelect: (TextRecognitionPipelineSelection?) -> Unit,
    onDownload: (List<ModelArtifactDescriptor>) -> Unit,
    onCancelDownload: (List<ModelArtifactDescriptor>) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(vertical = KatariTheme.spacing.s),
) {
    LazyColumn(modifier = modifier, contentPadding = contentPadding) {
        if (choices.currentUnavailable) {
            item(key = "unavailable") {
                Text(
                    text = stringResource(MR.strings.text_recognition_settings_choice_unavailable),
                    modifier = Modifier.padding(
                        horizontal = KatariTheme.spacing.rowInset,
                        vertical = KatariTheme.spacing.s,
                    ),
                    role = TextRole.Body,
                    tone = ContentTone.Error,
                )
            }
        }
        choices.automatic?.let { automatic ->
            item(key = "automatic") {
                val pipelineName = automatic.option.title ?: automatic.option.components.joinToString(" + ") {
                    it.displayName
                }
                TextRecognitionPipelineRow(
                    option = automatic.option,
                    title = stringResource(MR.strings.text_recognition_settings_automatic),
                    description = automatic.recommendedBy?.let {
                        stringResource(
                            MR.strings.text_recognition_settings_automatic_recommended,
                            pipelineName,
                            it.name,
                        )
                    } ?: pipelineName,
                    selected = selected == null,
                    onSelect = { onSelect(null) },
                    onDownload = onDownload,
                    onCancelDownload = onCancelDownload,
                )
            }
        }
        if (choices.presets.isNotEmpty()) {
            item { SectionHeader(stringResource(MR.strings.text_recognition_settings_presets)) }
            items(choices.presets, key = { it.selection.toString() }) { option ->
                TextRecognitionPipelineRow(option, option.selection == selected, onSelect, onDownload, onCancelDownload)
            }
        }
        if (choices.custom.isNotEmpty()) {
            item { SectionHeader(stringResource(MR.strings.text_recognition_settings_custom)) }
            items(choices.custom, key = { it.selection.toString() }) { option ->
                TextRecognitionPipelineRow(option, option.selection == selected, onSelect, onDownload, onCancelDownload)
            }
        }
    }
}

@Composable
private fun TextRecognitionPipelineRow(
    option: TextRecognitionPipelineOption,
    selected: Boolean,
    onSelect: (TextRecognitionPipelineSelection?) -> Unit,
    onDownload: (List<ModelArtifactDescriptor>) -> Unit,
    onCancelDownload: (List<ModelArtifactDescriptor>) -> Unit,
) {
    TextRecognitionPipelineRow(
        option = option,
        title = option.title ?: option.components.joinToString(" + ") { it.displayName },
        description = option.description ?: option.components.joinToString(" · ") { it.description },
        selected = selected,
        onSelect = { onSelect(option.selection) },
        onDownload = onDownload,
        onCancelDownload = onCancelDownload,
    )
}
