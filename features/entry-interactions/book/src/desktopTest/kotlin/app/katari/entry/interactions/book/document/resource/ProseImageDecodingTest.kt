package app.katari.entry.interactions.book.document.resource

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ProseImageDecodingTest {
    @Test
    fun `a truncated image fails although its header reads`() {
        val image = BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB).apply {
            for (x in 0 until width) for (y in 0 until height) setRGB(x, y, x * 4 shl 16 or y * 4)
        }
        val png = ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()

        decode(png).release()
        assertFailsWith<Exception> { decode(png.copyOf(png.size / 2)).release() }
    }

    private fun decode(bytes: ByteArray) = decodeValidatedProseImage(
        bytes = bytes,
        mediaType = "image/png",
        targetWidthPx = 1_024,
        targetHeightPx = 1_024,
        decoding = SkiaBookDocumentResourceDecoding,
    )
}
