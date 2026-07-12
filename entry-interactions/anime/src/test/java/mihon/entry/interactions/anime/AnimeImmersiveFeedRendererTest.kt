package mihon.entry.interactions.anime

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AnimeImmersiveFeedRendererTest {

    @Test
    fun `volume increase unmutes muted immersive video`() {
        shouldUnmuteAfterVolumeChange(
            muted = true,
            previousVolume = 2,
            currentVolume = 3,
        ) shouldBe true
    }

    @Test
    fun `unchanged or lower volume keeps immersive video muted`() {
        shouldUnmuteAfterVolumeChange(
            muted = true,
            previousVolume = 3,
            currentVolume = 3,
        ) shouldBe false
        shouldUnmuteAfterVolumeChange(
            muted = true,
            previousVolume = 3,
            currentVolume = 2,
        ) shouldBe false
    }

    @Test
    fun `volume increase leaves already audible immersive video unchanged`() {
        shouldUnmuteAfterVolumeChange(
            muted = false,
            previousVolume = 2,
            currentVolume = 3,
        ) shouldBe false
    }
}
