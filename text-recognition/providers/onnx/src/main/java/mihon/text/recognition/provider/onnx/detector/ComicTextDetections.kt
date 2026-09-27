package mihon.text.recognition.provider.onnx.detector

import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.spi.component.DetectedTextRegion
import mihon.text.recognition.spi.component.DetectedTextRegionKind
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Converts the detector's fixed-size query outputs into regions of a [width]×[height] bitmap. Queries below
 * [threshold], with unknown labels, or with boxes that collapse after clipping are dropped.
 *
 * @param boxes `xyxy` coordinates, four values per query, in bitmap pixels.
 */
internal fun decodeDetections(
    labels: LongArray,
    boxes: FloatArray,
    scores: FloatArray,
    width: Int,
    height: Int,
    threshold: Float = DEFAULT_THRESHOLD,
): List<DetectedTextRegion> {
    require(boxes.size == labels.size * 4 && scores.size == labels.size) { "Detector outputs disagree in size" }
    return labels.indices.mapNotNull { query ->
        val score = scores[query]
        if (score < threshold) return@mapNotNull null
        val kind = when (labels[query]) {
            LABEL_BUBBLE -> DetectedTextRegionKind.Bubble
            LABEL_BUBBLE_TEXT -> DetectedTextRegionKind.BubbleText
            LABEL_FREE_TEXT -> DetectedTextRegionKind.FreeText
            else -> return@mapNotNull null
        }
        val left = floor(boxes[query * 4]).toInt().coerceIn(0, width)
        val top = floor(boxes[query * 4 + 1]).toInt().coerceIn(0, height)
        val right = ceil(boxes[query * 4 + 2]).toInt().coerceIn(0, width)
        val bottom = ceil(boxes[query * 4 + 3]).toInt().coerceIn(0, height)
        if (right <= left || bottom <= top) return@mapNotNull null
        DetectedTextRegion(ImageRect(left, top, right, bottom), kind, score)
    }
}

private const val DEFAULT_THRESHOLD = 0.5f
private const val LABEL_BUBBLE = 0L
private const val LABEL_BUBBLE_TEXT = 1L
private const val LABEL_FREE_TEXT = 2L
