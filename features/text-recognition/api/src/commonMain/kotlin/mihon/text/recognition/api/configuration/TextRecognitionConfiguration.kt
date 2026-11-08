package mihon.text.recognition.api.configuration

import mihon.language.api.tag.LanguageTag
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.pipeline.TextRecognitionPipelineSelection
import mihon.text.recognition.api.pipeline.TextRecognitionPreset
import mihon.text.recognition.api.provider.TextRecognitionProviderId

/**
 * A profile's recognition choices: the engine used for every language it can read, and per-language overrides.
 *
 * Overrides apply to every regional variant of their language, because recognition reads scripts rather than
 * regions.
 */
data class TextRecognitionConfiguration(
    val provider: TextRecognitionProviderId?,
    val overrides: Map<LanguageTag, TextRecognitionPipelineSelection> = emptyMap(),
)

/** How a configuration reads one language. */
sealed interface TextRecognitionPipelineResolution {
    data class Resolved(
        val pipeline: TextRecognitionPipeline,
        val origin: TextRecognitionPipelineOrigin,
        val preset: TextRecognitionPreset? = null,
    ) : TextRecognitionPipelineResolution

    /** The override for the language names a preset or components this build does not have. */
    data class OverrideUnavailable(
        val selection: TextRecognitionPipelineSelection,
    ) : TextRecognitionPipelineResolution

    /** Components read the language, but neither an override nor any preset covers it. */
    data object ChoiceRequired : TextRecognitionPipelineResolution

    /** No component of this build reads the language. */
    data object UnsupportedLanguage : TextRecognitionPipelineResolution
}

enum class TextRecognitionPipelineOrigin {
    /** The profile overrides the pipeline for this language. */
    Override,

    /** The profile's engine recommends this pipeline for the language. */
    Engine,

    /** The profile's engine cannot read the language, so another engine's recommendation is used. */
    OtherEngine,
}
