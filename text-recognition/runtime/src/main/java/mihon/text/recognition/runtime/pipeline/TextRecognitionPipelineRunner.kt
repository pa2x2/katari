package mihon.text.recognition.runtime.pipeline

import mihon.language.api.tag.LanguageTag
import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.api.image.TextRecognitionImage
import mihon.text.recognition.api.result.RecognizedTextRegion
import mihon.text.recognition.spi.model.TextRecognitionModels

/** Executes one resolved pipeline. Returned regions are in source-image pixels, not yet in reading order. */
internal interface TextRecognitionPipelineRunner {
    /**
     * @param outlinedByUser whether [area] is an explicit user outline rather than the whole image, in which case text
     * the detector misses is still read from the area as a whole.
     */
    suspend fun run(
        image: TextRecognitionImage,
        area: ImageRect,
        outlinedByUser: Boolean,
        language: LanguageTag,
        models: TextRecognitionModels,
    ): List<RecognizedTextRegion>
}
