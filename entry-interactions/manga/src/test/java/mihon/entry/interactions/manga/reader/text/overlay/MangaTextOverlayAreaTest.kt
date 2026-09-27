package mihon.entry.interactions.manga.reader.text.overlay

import io.kotest.matchers.shouldBe
import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.api.result.RecognizedTextRegion
import mihon.text.recognition.api.result.TextOrientation
import mihon.text.recognition.api.result.TextRegionKind
import org.junit.jupiter.api.Test

class MangaTextOverlayAreaTest {

    @Test
    fun `a translation in a speech bubble may use the bubble interior around the original text`() {
        val region = RecognizedTextRegion(
            bounds = ImageRect(470, 120, 530, 380),
            text = "素直にあやまるしか",
            kind = TextRegionKind.SpeechBubble,
            orientation = TextOrientation.Vertical,
            container = ImageRect(400, 100, 600, 400),
        )

        overlayArea(region) shouldBe ImageRect(430, 120, 570, 380)
    }

    @Test
    fun `free text keeps to its own place so surrounding art stays visible`() {
        val region = RecognizedTextRegion(
            bounds = ImageRect(100, 1500, 600, 1550),
            text = "WHAT HAPPENED?",
            kind = TextRegionKind.FreeText,
            orientation = TextOrientation.Horizontal,
        )

        overlayArea(region) shouldBe ImageRect(80, 1480, 620, 1570)
    }
}
