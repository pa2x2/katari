package mihon.text.recognition.runtime.pipeline

import mihon.language.api.tag.LanguageTag
import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.api.image.TextRecognitionImage
import mihon.text.recognition.api.result.RecognizedTextRegion
import mihon.text.recognition.api.result.TextRegionKind
import mihon.text.recognition.runtime.geometry.enclosingContainer
import mihon.text.recognition.runtime.geometry.mergeOverlapping
import mihon.text.recognition.runtime.geometry.padded
import mihon.text.recognition.runtime.geometry.planTiles
import mihon.text.recognition.runtime.geometry.sampleSizeForMinimumEdge
import mihon.text.recognition.runtime.geometry.toSource
import mihon.text.recognition.spi.component.DetectedTextRegion
import mihon.text.recognition.spi.component.DetectedTextRegionKind
import mihon.text.recognition.spi.component.TextDetector
import mihon.text.recognition.spi.component.TextRecognizer
import mihon.text.recognition.spi.model.TextRecognitionModels

/** Runs a detector over tiles of the area, then reads each detected text region with a recognizer. */
internal class StagedPipelineRunner(
    private val detector: TextDetector,
    private val recognizer: TextRecognizer,
) : TextRecognitionPipelineRunner {

    override suspend fun run(
        image: TextRecognitionImage,
        area: ImageRect,
        outlinedByUser: Boolean,
        language: LanguageTag,
        models: TextRecognitionModels,
    ): List<RecognizedTextRegion> {
        val detections = mergeOverlapping(
            candidates = detect(image, area, models),
            bounds = DetectedTextRegion::bounds,
            withBounds = { detection, bounds -> detection.copy(bounds = bounds) },
            sameGroup = { first, second -> first.kind == second.kind },
            priority = compareByDescending(DetectedTextRegion::confidence),
        )
        val bubbles = detections.filter { it.kind == DetectedTextRegionKind.Bubble }.map(DetectedTextRegion::bounds)
        val texts = detections.mapNotNull { detection ->
            when (detection.kind) {
                DetectedTextRegionKind.Bubble -> null
                DetectedTextRegionKind.BubbleText -> detection.bounds to TextRegionKind.SpeechBubble
                DetectedTextRegionKind.FreeText -> detection.bounds to TextRegionKind.FreeText
            }
        }
        // An outlined area without detected text is still read as a whole: the user pointed at text the detector
        // missed.
        val regions = texts.ifEmpty { if (outlinedByUser) listOf(area to TextRegionKind.Unclassified) else emptyList() }
        return regions.mapNotNull { (bounds, kind) ->
            read(image, bounds, kind, enclosingContainer(bounds, bubbles), language, models)
        }
    }

    private suspend fun detect(
        image: TextRecognitionImage,
        area: ImageRect,
        models: TextRecognitionModels,
    ): List<DetectedTextRegion> {
        return planTiles(area).flatMap { tile ->
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

    private suspend fun read(
        image: TextRecognitionImage,
        bounds: ImageRect,
        kind: TextRegionKind,
        container: ImageRect?,
        language: LanguageTag,
        models: TextRecognitionModels,
    ): RecognizedTextRegion? {
        val crop = bounds.padded(CROP_PADDING, image.size.bounds)
        val bitmap = image.decodeRegion(crop, sampleSizeForMinimumEdge(crop, recognizer.inputEdge))
        val recognized = try {
            recognizer.recognize(bitmap, language, models)
        } finally {
            bitmap.recycle()
        }
        val text = recognized?.text?.trim()?.takeIf(String::isNotEmpty) ?: return null
        return RecognizedTextRegion(
            bounds = bounds,
            text = text,
            kind = kind,
            orientation = recognized.orientation,
            container = container,
        )
    }

    private companion object {
        const val CROP_PADDING = 0.02
    }
}
