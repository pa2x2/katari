package eu.kanade.tachiyomi.ui.video.player.components

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class VideoPlayerTimelineTest {

    @Test
    fun `horizontal preview fits available width without distortion`() {
        fitVideoPreviewSize(
            maxWidth = 300f,
            maxHeight = 200f,
            aspectRatio = 2f,
        ) shouldBe VideoPreviewSize(width = 300f, height = 150f)
    }

    @Test
    fun `vertical preview fits available height without distortion`() {
        fitVideoPreviewSize(
            maxWidth = 300f,
            maxHeight = 200f,
            aspectRatio = 0.5f,
        ) shouldBe VideoPreviewSize(width = 100f, height = 200f)
    }

    @Test
    fun `invalid preview dimensions collapse safely`() {
        fitVideoPreviewSize(
            maxWidth = 0f,
            maxHeight = 200f,
            aspectRatio = 1f,
        ) shouldBe VideoPreviewSize(width = 0f, height = 0f)
    }
}
