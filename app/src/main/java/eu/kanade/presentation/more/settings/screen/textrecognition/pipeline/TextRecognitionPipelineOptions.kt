package eu.kanade.presentation.more.settings.screen.textrecognition.pipeline

import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.api.state.ModelArtifactState
import mihon.text.recognition.api.component.KnownTextRecognitionComponent
import mihon.text.recognition.api.component.TextRecognitionBuildAvailability
import mihon.text.recognition.api.pipeline.TextRecognitionPipelineSelection

/** A pipeline the user can pick for one language, with the models it needs and their current state. */
internal data class TextRecognitionPipelineOption(
    val selection: TextRecognitionPipelineSelection,
    val title: String,
    val description: String?,
    val components: List<KnownTextRecognitionComponent>,
    val models: List<Pair<ModelArtifactDescriptor, ModelArtifactState>>,
) {
    /** Reason the option cannot run in this build, if any of its components is excluded. */
    val exclusion: TextRecognitionBuildAvailability.NotIncluded?
        get() = components.firstNotNullOfOrNull {
            it.buildAvailability as? TextRecognitionBuildAvailability.NotIncluded
        }

    val missingModels: List<ModelArtifactDescriptor>
        get() = models.filter { (_, state) ->
            state !is ModelArtifactState.Installed &&
                state !is ModelArtifactState.Downloading
        }
            .map { it.first }
}

internal data class TextRecognitionPipelinePickerState(
    val language: LanguageTag,
    val explicitSelection: TextRecognitionPipelineSelection?,
    val defaultSelection: TextRecognitionPipelineSelection?,
    val presets: List<TextRecognitionPipelineOption>,
    val customPipelines: List<TextRecognitionPipelineOption>,
) {
    val effectiveSelection: TextRecognitionPipelineSelection?
        get() = explicitSelection ?: defaultSelection
}
