package app.katari.text.recognition.provider.onnx.paddle.lines

import app.katari.text.recognition.api.image.ArgbPixels
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class LineCropTest {

    /** A 6×4 image whose every pixel holds its own coordinates, so a crop shows where each pixel came from. */
    private val source = ArgbPixels(6, 4, IntArray(24) { index -> pixel(x = index % 6, y = index / 6) })

    private fun pixel(x: Int, y: Int) = (0xFF shl 24) or (x shl 16) or (y shl 8)

    private fun line(left: Double, top: Double, right: Double, bottom: Double) = RotatedRectangle.Quadrilateral(
        listOf(
            RotatedRectangle.Point(left, top),
            RotatedRectangle.Point(right, top),
            RotatedRectangle.Point(right, bottom),
            RotatedRectangle.Point(left, bottom),
        ),
    )

    @Test
    fun `an upright line is cut out pixel for pixel`() {
        val crop = cropLine(source, line(1.0, 1.0, 4.0, 3.0), turnCounterClockwise = false)!!

        crop.width shouldBe 3
        crop.height shouldBe 2
        crop.values.toList() shouldBe listOf(
            pixel(1, 1),
            pixel(2, 1),
            pixel(3, 1),
            pixel(1, 2),
            pixel(2, 2),
            pixel(3, 2),
        )
    }

    @Test
    fun `a column turned counter-clockwise runs from left to right what ran from top to bottom`() {
        val crop = cropLine(source, line(2.0, 0.0, 3.0, 3.0), turnCounterClockwise = true)!!

        crop.width shouldBe 3
        crop.height shouldBe 1
        crop.values.toList() shouldBe listOf(pixel(2, 0), pixel(2, 1), pixel(2, 2))
    }

    @Test
    fun `a line reaching past the image repeats its edge pixels`() {
        val crop = cropLine(source, line(4.0, 2.0, 8.0, 3.0), turnCounterClockwise = false)!!

        crop.values.toList() shouldBe listOf(pixel(4, 2), pixel(5, 2), pixel(5, 2), pixel(5, 2))
    }
}
