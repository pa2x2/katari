package eu.kanade.tachiyomi.ui.video.player

import androidx.annotation.OptIn
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

@OptIn(markerClass = [UnstableApi::class])
class VideoPlayerVideoSizeTest {

    @Test
    fun `vertical video reports a portrait aspect ratio`() {
        VideoSize(1080, 1920).displayAspectRatioOrNull() shouldBe 0.5625f
    }

    @Test
    fun `video aspect ratio includes non-square pixels`() {
        VideoSize(720, 576, 1.25f).displayAspectRatioOrNull() shouldBe 1.5625f
    }

    @Test
    fun `unknown video size has no aspect ratio`() {
        VideoSize.UNKNOWN.displayAspectRatioOrNull() shouldBe null
    }
}
