package mihon.text.recognition.ui.languages

import androidx.compose.runtime.Composable
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.api.state.ModelArtifactState
import mihon.model.artifacts.ui.state.formatModelArtifactSize
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Whether the models a row's pipeline needs are downloaded, downloading, or still missing and how large they are. */
@Composable
internal fun TextRecognitionLanguageModelStatus(
    models: List<ModelArtifactDescriptor>,
    states: Map<ModelArtifactDescriptor, ModelArtifactState>,
) {
    if (models.isEmpty()) return
    val modelStates = models.map { it to (states[it] ?: ModelArtifactState.NotInstalled) }
    val missing = modelStates.filter { (_, state) ->
        state !is ModelArtifactState.Installed && state !is ModelArtifactState.Downloading
    }
    when {
        modelStates.any { (_, state) -> state is ModelArtifactState.Downloading } -> Text(
            text = stringResource(MR.strings.text_recognition_languages_models_downloading),
            role = TextRole.Supporting,
            tone = ContentTone.Primary,
        )
        missing.isNotEmpty() -> Text(
            text = stringResource(
                MR.strings.text_recognition_languages_models_needed,
                formatModelArtifactSize(missing.sumOf { (model, _) -> model.sizeBytes }),
            ),
            role = TextRole.Supporting,
            tone = ContentTone.Primary,
        )
        else -> Text(
            text = stringResource(MR.strings.text_recognition_languages_models_downloaded),
            role = TextRole.Supporting,
        )
    }
}
