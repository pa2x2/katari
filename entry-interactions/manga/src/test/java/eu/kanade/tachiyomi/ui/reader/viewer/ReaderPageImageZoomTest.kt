package eu.kanade.tachiyomi.ui.reader.viewer

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ReaderPageImageZoomTest {

    @Test
    fun `fitted image is not zoomed after returning from zoom`() {
        isScaleZoomed(scale = 0.5F, minimumScale = 0.5F) shouldBe false
        isScaleZoomed(scale = 0.5001F, minimumScale = 0.5F) shouldBe false
    }

    @Test
    fun `scale above fitted minimum is zoomed`() {
        isScaleZoomed(scale = 0.51F, minimumScale = 0.5F) shouldBe true
    }
}
