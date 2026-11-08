package mihon.text.recognition.runtime.geometry

import io.kotest.matchers.collections.shouldContainExactly
import mihon.text.recognition.api.image.ImageRect
import org.junit.jupiter.api.Test

class RegionMergingTest {

    private data class Detection(val bounds: ImageRect, val confidence: Double)

    private fun merge(vararg detections: Detection) = mergeOverlapping(
        candidates = detections.toList(),
        bounds = Detection::bounds,
        withBounds = { detection, bounds -> detection.copy(bounds = bounds) },
        priority = compareByDescending(Detection::confidence),
    )

    @Test
    fun `a region that grows to cover another accepted region absorbs it`() {
        // Two lines of a list are accepted apart; a less confident detection of the whole list then joins the first
        // line, and the grown line now covers the second.
        val upperLine = Detection(ImageRect(320, 1158, 576, 1234), confidence = 0.56)
        val lowerLine = Detection(ImageRect(310, 1261, 617, 1452), confidence = 0.50)
        val wholeList = Detection(ImageRect(310, 1088, 612, 1458), confidence = 0.46)

        merge(upperLine, lowerLine, wholeList) shouldContainExactly
            listOf(upperLine.copy(bounds = ImageRect(310, 1088, 617, 1458)))
    }
}
