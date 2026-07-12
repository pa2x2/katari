package eu.kanade.tachiyomi.ui.video.player.components

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class VideoPlayerInsetsTest {

    @Test
    fun `asymmetric horizontal insets produce a centered safe frame`() {
        resolveVideoPlayerSafeInsets(
            left = 84,
            top = 0,
            right = 0,
            bottom = 0,
        ) shouldBe VideoPlayerSafeInsets(
            horizontal = 84,
            top = 0,
            bottom = 0,
        )
    }

    @Test
    fun `top cutout remains when system bars are hidden`() {
        resolveVideoPlayerSafeInsets(
            left = 0,
            top = 96,
            right = 0,
            bottom = 0,
        ) shouldBe VideoPlayerSafeInsets(
            horizontal = 0,
            top = 96,
            bottom = 0,
        )
    }

    @Test
    fun `invalid inset values are clamped`() {
        resolveVideoPlayerSafeInsets(
            left = -1,
            top = -2,
            right = -3,
            bottom = -4,
        ) shouldBe VideoPlayerSafeInsets(
            horizontal = 0,
            top = 0,
            bottom = 0,
        )
    }
}
