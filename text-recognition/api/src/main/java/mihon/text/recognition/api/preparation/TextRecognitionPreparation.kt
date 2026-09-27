package mihon.text.recognition.api.preparation

import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.text.recognition.api.component.TextRecognitionComponentId
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.pipeline.TextRecognitionPreset

/** Opaque, process-local execution authority returned only by [mihon.text.recognition.api.TextRecognitionFeature.prepare]. */
interface ReadyTextRecognition

sealed interface TextRecognitionPreparation {
    data class Ready(
        val recognition: ReadyTextRecognition,
        val language: LanguageTag,
        val pipeline: TextRecognitionPipeline,
    ) : TextRecognitionPreparation

    /** The request did not state the language of the text; the host must ask for it. */
    data class LanguageRequired(
        val supportedLanguages: List<LanguageTag>,
    ) : TextRecognitionPreparation

    /** No usable pipeline is selected for [language]; the host must let the user choose one. */
    data class PipelineChoiceRequired(
        val language: LanguageTag,
        val reason: TextRecognitionPipelineChoiceReason,
        val presets: List<TextRecognitionPreset>,
    ) : TextRecognitionPreparation

    /** The pipeline needs models that are not installed. They are downloaded only after the user approves. */
    data class ModelsRequired(
        val language: LanguageTag,
        val pipeline: TextRecognitionPipeline,
        val models: List<ModelArtifactDescriptor>,
    ) : TextRecognitionPreparation {
        init {
            require(models.isNotEmpty())
        }
    }

    data class Unavailable(
        val reason: TextRecognitionUnavailableReason,
    ) : TextRecognitionPreparation
}

sealed interface TextRecognitionPipelineChoiceReason {
    data object NothingSelected : TextRecognitionPipelineChoiceReason

    data class SelectedComponentUnavailable(
        val component: TextRecognitionComponentId,
    ) : TextRecognitionPipelineChoiceReason
}

sealed interface TextRecognitionUnavailableReason {
    /** No component in this build reads [language]. */
    data class UnsupportedLanguage(
        val language: LanguageTag,
    ) : TextRecognitionUnavailableReason

    data class ComponentUnavailable(
        val component: TextRecognitionComponentId,
        val reason: String,
    ) : TextRecognitionUnavailableReason {
        init {
            require(reason.isNotBlank())
        }
    }
}
