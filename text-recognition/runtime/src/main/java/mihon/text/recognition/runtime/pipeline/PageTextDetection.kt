package mihon.text.recognition.runtime.pipeline

import mihon.language.api.tag.LanguageTag
import mihon.text.recognition.api.image.TextRecognitionImage
import mihon.text.recognition.runtime.cache.TextDetectionCache
import mihon.text.recognition.runtime.cache.TextDetectionCacheKey
import mihon.text.recognition.runtime.geometry.planTiles
import mihon.text.recognition.runtime.geometry.sampleSizeForMinimumEdge
import mihon.text.recognition.runtime.geometry.toSource
import mihon.text.recognition.spi.component.DetectedTextRegion
import mihon.text.recognition.spi.component.TextDetector
import mihon.text.recognition.spi.model.TextRecognitionModels

/**
 * Runs a detector over the whole page, tile by tile, and returns its detections in source pixels, unmerged.
 *
 * The detector always sees the whole page: it was trained on pages, and a small area stretched to its input is text
 * at a scale it has not seen. A page's detections are therefore the same for every request on it and are reused.
 */
internal class PageTextDetection(
    private val cache: TextDetectionCache,
) {
    suspend fun detect(
        image: TextRecognitionImage,
        detector: TextDetector,
        language: LanguageTag,
        models: TextRecognitionModels,
    ): List<DetectedTextRegion> {
        val key = TextDetectionCacheKey(
            image = image.key,
            size = image.size,
            detector = detector.catalogEntry.id,
            processingRevision = detector.processingRevision,
            models = detector.models(language).associate { it.id to it.revision },
        )
        cache[key]?.let { return it }
        return detectTiles(image, detector, models).also { cache[key] = it }
    }

    private suspend fun detectTiles(
        image: TextRecognitionImage,
        detector: TextDetector,
        models: TextRecognitionModels,
    ): List<DetectedTextRegion> {
        return planTiles(image.size.bounds, minimumLength = detector.inputEdge).flatMap { tile ->
            val sampleSize = sampleSizeForMinimumEdge(tile, detector.inputEdge)
            val bitmap = image.decodeRegion(tile, sampleSize)
            try {
                detector.detect(bitmap, models).mapNotNull { detection ->
                    detection.bounds.toSource(tile, sampleSize)?.let { detection.copy(bounds = it) }
                }
            } finally {
                bitmap.recycle()
            }
        }
    }
}
