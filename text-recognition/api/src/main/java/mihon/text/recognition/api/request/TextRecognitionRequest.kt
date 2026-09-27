package mihon.text.recognition.api.request

import mihon.language.api.tag.LanguageTag
import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.api.image.TextRecognitionImage

/**
 * @property language the language of the text in the image. Recognition pipelines are chosen per language, so an
 * unknown language is reported as a preparation requirement instead of being guessed.
 */
data class TextRecognitionRequest(
    val image: TextRecognitionImage,
    val language: LanguageTag?,
    val scope: TextRecognitionScope = TextRecognitionScope.WholeImage,
)

sealed interface TextRecognitionScope {
    /** Every text region of the image, in reading order. */
    data object WholeImage : TextRecognitionScope

    /** Only text inside [region], for example an area the user outlined. */
    data class Region(
        val region: ImageRect,
    ) : TextRecognitionScope
}
