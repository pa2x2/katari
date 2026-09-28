package mihon.text.recognition.runtime.pipeline

import mihon.language.api.tag.LanguageTag
import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.api.image.TextRecognitionImage
import mihon.text.recognition.api.result.RecognizedTextRegion
import mihon.text.recognition.api.result.TextRegionKind
import mihon.text.recognition.runtime.geometry.coverage
import mihon.text.recognition.runtime.geometry.enclosingContainer
import mihon.text.recognition.runtime.geometry.mergeOverlapping
import mihon.text.recognition.runtime.geometry.padded
import mihon.text.recognition.runtime.geometry.sampleSizeForMinimumEdge
import mihon.text.recognition.spi.component.DetectedTextRegion
import mihon.text.recognition.spi.component.DetectedTextRegionKind
import mihon.text.recognition.spi.component.TextDetector
import mihon.text.recognition.spi.component.TextRecognizer
import mihon.text.recognition.spi.model.TextRecognitionModels

/**
 * Detects text on the whole page, then reads the text of each speech bubble, and each text outside bubbles, with a
 * recognizer. For an outlined area, the page detections the outline covers are read.
 */
internal class StagedPipelineRunner(
    private val detector: TextDetector,
    private val recognizer: TextRecognizer,
    private val detection: PageTextDetection,
) {

    /**
     * Returns regions in source-image pixels, not yet in reading order.
     *
     * @param outlinedByUser whether [area] is an explicit user outline rather than the whole image, in which case text
     * the detector misses is still read from the area as a whole.
     */
    suspend fun run(
        image: TextRecognitionImage,
        area: ImageRect,
        outlinedByUser: Boolean,
        language: LanguageTag,
        models: TextRecognitionModels,
    ): List<RecognizedTextRegion> {
        // Bubble outlines and text are different objects; text labelled as inside and outside a bubble is the same.
        val detections = mergeOverlapping(
            candidates = detection.detect(image, detector, language, models),
            bounds = DetectedTextRegion::bounds,
            withBounds = { detection, bounds -> detection.copy(bounds = bounds) },
            sameGroup = { first, second -> first.isBubble == second.isBubble },
            priority = compareByDescending(DetectedTextRegion::confidence),
        )
        val bubbles = detections.filter(DetectedTextRegion::isBubble).map(DetectedTextRegion::bounds)
        val texts = detections
            .filterNot(DetectedTextRegion::isBubble)
            .filter { !outlinedByUser || coverage(it.bounds, area) >= OUTLINED_COVERAGE }
        val readings = readings(texts, bubbles)
        // An outlined area without detected text is still read as a whole: the user pointed at text the detector
        // missed.
        return readings
            .ifEmpty { if (outlinedByUser) listOf(Reading(area, TextRegionKind.Unclassified, null)) else emptyList() }
            .mapNotNull { read(image, it, language, models) }
    }

    /**
     * One reading per speech bubble, covering all of its text, and one per text outside bubbles. A bubble's text
     * split into several detections is one utterance: read apart, each piece loses the context of the others.
     */
    private fun readings(texts: List<DetectedTextRegion>, bubbles: List<ImageRect>): List<Reading> {
        val (inBubbles, outside) = texts
            .map { text -> text to enclosingContainer(text.bounds, bubbles) }
            .partition { (_, bubble) -> bubble != null }
        return inBubbles.groupBy({ (_, bubble) -> bubble!! }, { (text, _) -> text }).map { (bubble, pieces) ->
            Reading(
                bounds = pieces.map(DetectedTextRegion::bounds).reduce(ImageRect::union),
                kind = TextRegionKind.SpeechBubble,
                container = bubble,
            )
        } + outside.map { (text, _) ->
            Reading(
                bounds = text.bounds,
                kind = if (text.kind == DetectedTextRegionKind.BubbleText) {
                    TextRegionKind.SpeechBubble
                } else {
                    TextRegionKind.FreeText
                },
                container = null,
            )
        }
    }

    private suspend fun read(
        image: TextRecognitionImage,
        reading: Reading,
        language: LanguageTag,
        models: TextRecognitionModels,
    ): RecognizedTextRegion? {
        val crop = reading.bounds.padded(CROP_PADDING, image.size.bounds)
        val bitmap = image.decodeRegion(crop, sampleSizeForMinimumEdge(crop, recognizer.inputEdge))
        val recognized = try {
            recognizer.recognize(bitmap, language, models)
        } finally {
            bitmap.recycle()
        }
        val text = recognized?.text?.trim()?.takeIf(String::isNotEmpty) ?: return null
        return RecognizedTextRegion(
            bounds = reading.bounds,
            text = text,
            kind = reading.kind,
            orientation = recognized.orientation,
            container = reading.container,
        )
    }

    private class Reading(
        val bounds: ImageRect,
        val kind: TextRegionKind,
        val container: ImageRect?,
    )

    private companion object {
        const val CROP_PADDING = 0.02

        /** Share of a detected text that must lie inside an outline for the outline to select it. */
        const val OUTLINED_COVERAGE = 0.5
    }
}

private val DetectedTextRegion.isBubble: Boolean
    get() = kind == DetectedTextRegionKind.Bubble
