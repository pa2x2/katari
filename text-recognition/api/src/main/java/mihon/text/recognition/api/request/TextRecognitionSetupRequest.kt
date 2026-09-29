package mihon.text.recognition.api.request

import mihon.language.api.tag.LanguageTag
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline

/**
 * The part of a [TextRecognitionRequest] that decides whether recognition can run, without an image.
 *
 * @property pipeline checks this pipeline instead of the profile's choice.
 */
data class TextRecognitionSetupRequest(
    val language: LanguageTag?,
    val pipeline: TextRecognitionPipeline? = null,
)
