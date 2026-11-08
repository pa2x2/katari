package app.katari.text.recognition.runtime.pipeline

import app.katari.language.api.tag.LanguageTag
import app.katari.text.recognition.api.image.TextRecognitionImage
import app.katari.text.recognition.api.image.release
import app.katari.text.recognition.runtime.cache.TextDetectionCache
import app.katari.text.recognition.runtime.cache.TextDetectionCacheKey
import app.katari.text.recognition.runtime.geometry.planTiles
import app.katari.text.recognition.runtime.geometry.sampleSizeForMinimumEdge
import app.katari.text.recognition.runtime.geometry.toSource
import app.katari.text.recognition.spi.component.DetectedTextRegion
import app.katari.text.recognition.spi.component.TextDetector
import app.katari.text.recognition.spi.model.TextRecognitionModels

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
                bitmap.release()
            }
        }
    }
}
