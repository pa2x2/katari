package eu.kanade.tachiyomi.ui.reader.viewer

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ReaderPageImageZoomTest {

    @Test
    fun `a fitted image with rounding drift after returning from zoom is not zoomed`() {
        isScaleZoomed(scale = 0.5001F, minimumScale = 0.5F) shouldBe false
        isScaleZoomed(scale = 0.51F, minimumScale = 0.5F) shouldBe true
    }
}
