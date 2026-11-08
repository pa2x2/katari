package mihon.video.engine.mpv.tracks

import eu.kanade.tachiyomi.source.entry.VideoRequest
import eu.kanade.tachiyomi.source.entry.VideoSubtitle
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class MpvVideoTracksTest {

    // An MKV's tracks as mpv lists them, in ISO 639-2 (bibliographic `fre` included), and a source's subtitle added.
    private val tracks = MpvTrack.parseList(
        """
        [
          {"id":1,"type":"audio","lang":"jpn","selected":true},
          {"id":1,"type":"sub","lang":"eng","title":"Signs","codec":"ass","default":true},
          {"id":2,"type":"sub","lang":"fre","codec":"subrip"},
          {"id":3,"type":"sub","codec":"subrip","external":true,
           "external-filename":"http://127.0.0.1/s0"}
        ]
        """.trimIndent(),
    )
    private val subtitle = VideoSubtitle(
        VideoRequest("https://cdn.example.com/pt.vtt"),
        label = "Português",
        language = "pt_BR",
        key = "pt",
    )

    @Test
    fun `languages are ExoPlayer's, so saved subtitle picks match on both engines, and external ids keep their keys`() {
        val published = mpvVideoTracks(
            tracks = tracks,
            externalSubtitles = listOf(MpvExternalSubtitle(0, "http://127.0.0.1/s0", subtitle)),
            externalAudio = emptyList(),
            variantSets = emptyList(),
            positionMs = 0L,
        ).groups.map { it.tracks.single() }

        published.map { it.language } shouldBe listOf("ja", "en", "fr", "pt-br")
        published.last().id shouldBe "0:pt"
        published[1].mimeType shouldBe "text/x-ssa"
    }
}
