package mihon.text.recognition.runtime.geometry

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
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
    fun `a region seen whole in one tile and truncated in the next is reported once with its full extent`() {
        val whole = Detection(ImageRect(100, 1000, 400, 1300), confidence = 0.9)
        val truncated = Detection(ImageRect(100, 1012, 400, 1300), confidence = 0.6)

        merge(truncated, whole) shouldContainExactly listOf(whole)
    }

    @Test
    fun `a region cut in two by a tile edge is restored from both halves`() {
        val upper = Detection(ImageRect(100, 1000, 400, 1200), confidence = 0.8)
        val lower = Detection(ImageRect(100, 1050, 400, 1300), confidence = 0.7)

        merge(upper, lower) shouldContainExactly listOf(upper.copy(bounds = ImageRect(100, 1000, 400, 1300)))
    }

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

    @Test
    fun `adjacent bubbles that barely touch stay separate`() {
        val left = Detection(ImageRect(0, 0, 200, 200), confidence = 0.9)
        val right = Detection(ImageRect(190, 0, 400, 200), confidence = 0.9)

        merge(left, right) shouldContainExactly listOf(left, right)
    }

    @Test
    fun `text belongs to the tightest bubble that encloses it`() {
        val text = ImageRect(120, 120, 180, 220)
        val bubble = ImageRect(100, 100, 200, 240)
        val panel = ImageRect(0, 0, 600, 600)
        val elsewhere = ImageRect(300, 300, 400, 400)

        enclosingContainer(text, listOf(panel, bubble, elsewhere)) shouldBe bubble
        enclosingContainer(text, listOf(elsewhere)) shouldBe null
    }
}
