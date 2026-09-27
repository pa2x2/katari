package mihon.text.recognition.api.request

import mihon.language.api.tag.LanguageTag
import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.api.image.TextRecognitionImage
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline

/**
 * @property language the language of the text in the image. Recognition pipelines are chosen per language, so an
 * unknown language is reported as a preparation requirement instead of being guessed.
 * @property pipeline runs this pipeline instead of the profile's choice, for example to try an unsaved configuration.
 */
data class TextRecognitionRequest(
    val image: TextRecognitionImage,
    val language: LanguageTag?,
    val scope: TextRecognitionScope = TextRecognitionScope.WholeImage,
    val pipeline: TextRecognitionPipeline? = null,
)

sealed interface TextRecognitionScope {
    /** Every text region of the image, in reading order. */
    data object WholeImage : TextRecognitionScope

    /** Only text inside [region], for example an area the user outlined. */
    data class Region(
        val region: ImageRect,
    ) : TextRecognitionScope
}
