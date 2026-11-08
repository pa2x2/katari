package eu.kanade.tachiyomi.ui.video.player.recovery

import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class VideoUnofferedPickTest {

    @Test
    fun `only a dub or server the viewer changed and the source answered with another counts as not offered`() {
        val playing = PlaybackSelection(dubKey = "studio", serverKey = "hls")
        val original = playing.copy(dubKey = "original")

        isUnofferedPick(listed = playing, picked = original, answered = playing) shouldBe true
        isUnofferedPick(listed = playing, picked = original, answered = original) shouldBe false
        // The server carried over from the playing dub wasn't picked, so another one is the dub's to choose.
        isUnofferedPick(listed = playing, picked = original, answered = original.copy(serverKey = "mp4")) shouldBe false

        val mp4 = playing.copy(serverKey = "mp4")
        isUnofferedPick(listed = playing, picked = mp4, answered = playing) shouldBe true
    }
}
