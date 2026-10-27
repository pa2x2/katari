package mihon.text.recognition.ui.picker.pipeline

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.icerock.moko.resources.StringResource
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.presentation.kit.layout.RowList
import mihon.presentation.kit.list.RowGroupGap
import mihon.presentation.kit.list.RowGroupHeader
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupNote
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.list.rowGroupItems
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
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
    RowList(modifier = modifier, contentPadding = contentPadding) {
        if (choices.currentUnavailable) {
            item(key = "unavailable") {
                RowGroupNote(
                    text = stringResource(MR.strings.text_recognition_settings_choice_unavailable),
                    tone = ContentTone.Error,
                )
            }
        }
        choices.automatic?.let { automatic ->
            item(key = "automatic") {
                val pipelineName = automatic.option.title ?: automatic.option.components.joinToString(" + ") {
                    it.displayName
                }
                RowGroupItem(position = RowGroupPosition.Alone) {
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
        }
        pipelineSection(
            key = "presets",
            title = MR.strings.text_recognition_settings_presets,
            options = choices.presets,
            gapAbove = choices.automatic != null,
        ) {
            TextRecognitionPipelineRow(it, it.selection == selected, onSelect, onDownload, onCancelDownload)
        }
        pipelineSection(
            key = "custom",
            title = MR.strings.text_recognition_settings_custom,
            options = choices.custom,
            gapAbove = choices.automatic != null || choices.presets.isNotEmpty(),
        ) {
            TextRecognitionPipelineRow(it, it.selection == selected, onSelect, onDownload, onCancelDownload)
        }
    }
}

private fun LazyListScope.pipelineSection(
    key: String,
    title: StringResource,
    options: List<TextRecognitionPipelineOption>,
    gapAbove: Boolean,
    row: @Composable (TextRecognitionPipelineOption) -> Unit,
) {
    if (options.isEmpty()) return
    if (gapAbove) item(key = "$key-gap") { RowGroupGap() }
    item(key = "$key-heading") { RowGroupHeader(stringResource(title)) }
    rowGroupItems(options, key = { it.selection.toString() }, itemContent = row)
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
