package mihon.entry.interactions.manga.reader.text.geometry

import io.kotest.matchers.shouldBe
import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.api.image.ImageSize
import mihon.text.recognition.api.result.RecognizedTextRegion
import mihon.text.recognition.api.result.TextOrientation
import mihon.text.recognition.api.result.TextRegionKind
import org.junit.jupiter.api.Test
import tachiyomi.core.common.util.system.ImageUtil

class MangaDisplayedPageGeometryTest {

    // An odd width: the right half starts one pixel past the middle, as the viewers cut it.
    private val spread = ImageSize(1001, 600)

    @Test
    fun `split spreads show each region on the half holding most of it, clipped to that half`() {
        val acrossGutter = region(ImageRect(480, 100, 560, 200), container = ImageRect(450, 50, 600, 250))
        val onLeftPage = region(ImageRect(100, 100, 200, 200))
        val onRightPage = region(ImageRect(600, 100, 700, 200))

        val rightHalf = geometry(MangaPageTransform.Half(spread, ImageUtil.Side.RIGHT))
        rightHalf.toDisplayed(acrossGutter)?.bounds shouldBe ImageRect(0, 100, 59, 200)
        rightHalf.toDisplayed(acrossGutter)?.container shouldBe ImageRect(0, 50, 99, 250)
        geometry(MangaPageTransform.Half(spread, ImageUtil.Side.LEFT)).toDisplayed(acrossGutter) shouldBe null

        val stacked = geometry(MangaPageTransform.Stacked(spread, upper = ImageUtil.Side.RIGHT))
        stacked.toDisplayed(onRightPage)?.bounds shouldBe ImageRect(99, 100, 199, 200)
        stacked.toDisplayed(onLeftPage)?.bounds shouldBe ImageRect(100, 700, 200, 800)
    }

    @Test
    fun `rotated spreads turn regions the way the page turned, and cropped borders shift them`() {
        val nearTopLeft = region(ImageRect(100, 50, 300, 150))

        // Clockwise, the raw top edge becomes the right edge; counterclockwise, the left edge.
        geometry(MangaPageTransform.Rotated(spread.copy(width = 1000), clockwise = true))
            .toDisplayed(nearTopLeft)?.bounds shouldBe ImageRect(450, 100, 550, 300)
        geometry(MangaPageTransform.Rotated(spread.copy(width = 1000), clockwise = false))
            .toDisplayed(nearTopLeft)?.bounds shouldBe ImageRect(50, 700, 150, 900)

        val cropped = MangaDisplayedPageGeometry(
            transform = MangaPageTransform.Rotated(spread.copy(width = 1000), clockwise = true),
            shown = ImageRect(20, 30, 580, 980),
        )
        cropped.toDisplayed(nearTopLeft)?.bounds shouldBe ImageRect(430, 70, 530, 270)
    }

    private fun geometry(transform: MangaPageTransform) =
        MangaDisplayedPageGeometry(transform, shown = ImageRect(0, 0, 10_000, 10_000))

    private fun region(bounds: ImageRect, container: ImageRect? = null) = RecognizedTextRegion(
        bounds = bounds,
        text = "text",
        kind = TextRegionKind.SpeechBubble,
        orientation = TextOrientation.Vertical,
        container = container,
    )
}
