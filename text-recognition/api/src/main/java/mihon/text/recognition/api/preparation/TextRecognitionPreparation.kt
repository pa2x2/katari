package mihon.text.recognition.api.preparation

import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.text.recognition.api.component.TextRecognitionComponentId
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.pipeline.TextRecognitionPreset

/** Opaque, process-local execution authority returned only by [mihon.text.recognition.api.TextRecognitionFeature.prepare]. */
interface ReadyTextRecognition

sealed interface TextRecognitionPreparation {
    /** Recognition of the requested image may run. */
    data class Ready(
        val recognition: ReadyTextRecognition,
        val language: LanguageTag,
        val pipeline: TextRecognitionPipeline,
    ) : TextRecognitionPreparation

    /** The request did not state the language of the text; the host must ask for it. */
    data class LanguageRequired(
        val supportedLanguages: List<LanguageTag>,
    ) : TextRecognitionRequirement

    /** No usable pipeline is selected for [language]; the host must let the user choose one. */
    data class PipelineChoiceRequired(
        val language: LanguageTag,
        val reason: TextRecognitionPipelineChoiceReason,
        val presets: List<TextRecognitionPreset>,
    ) : TextRecognitionRequirement

    /** The pipeline needs models that are not installed. They are downloaded only after the user approves. */
    data class ModelsRequired(
        val language: LanguageTag,
        val pipeline: TextRecognitionPipeline,
        val models: List<ModelArtifactDescriptor>,
    ) : TextRecognitionRequirement {
        init {
            require(models.isNotEmpty())
        }
    }

    /**
     * The pipeline needs models the platform downloads (for example through Google Play services). Hosts ask the user
     * and then call [mihon.text.recognition.api.host.TextRecognitionHostActions.installPlatformModels].
     */
    data class PlatformModelsRequired(
        val language: LanguageTag,
        val pipeline: TextRecognitionPipeline,
        val component: TextRecognitionComponentId,
        val description: String,
        val approximateSizeBytes: Long? = null,
    ) : TextRecognitionRequirement {
        init {
            require(description.isNotBlank())
        }
    }

    data class Unavailable(
        val reason: TextRecognitionUnavailableReason,
    ) : TextRecognitionRequirement
}

/**
 * A prerequisite the user must resolve before recognition can run. The same requirements block recognizing an image
 * and preparing a setup without one.
 */
sealed interface TextRecognitionRequirement :
    TextRecognitionPreparation,
    TextRecognitionSetupPreparation

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
