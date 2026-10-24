package mihon.video.engine.mpv.player

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class MpvFileOptionsTest {

    @Test
    fun `a value with commas or non-ASCII text stays one option, and a name that would inject another is dropped`() {
        val options = linkedMapOf(
            "demuxer-lavf-o" to "headers=Referer: https://a.example/,timeout=5",
            "force-media-title" to "Серия 1",
            "start=0,pause" to "yes",
        )

        loadfileOptions(options) shouldBe
            "demuxer-lavf-o=%45%headers=Referer: https://a.example/,timeout=5,force-media-title=%12%Серия 1"
    }
}
