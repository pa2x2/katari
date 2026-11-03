package mihon.text.recognition.api.preparation

import mihon.language.api.tag.LanguageTag
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline

/**
 * Whether recognition could run for a language before any image exists, for example to settle prerequisites before
 * work is scheduled. Only [Ready] differs from image preparation; every other outcome is a [TextRecognitionRequirement].
 */
sealed interface TextRecognitionSetupPreparation {
    /** Recognition of [language] would run [pipeline]; pass both to a later request to use exactly this setup. */
    data class Ready(
        val language: LanguageTag,
        val pipeline: TextRecognitionPipeline,
    ) : TextRecognitionSetupPreparation
}
