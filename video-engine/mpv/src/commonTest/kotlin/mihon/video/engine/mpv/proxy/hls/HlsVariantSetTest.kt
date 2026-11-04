package mihon.video.engine.mpv.proxy.hls

import io.kotest.matchers.shouldBe
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test

class HlsVariantSetTest {

    private fun variant(bandwidth: Long, height: Int, name: String): HlsLoadedVariant {
        val playlist = """
            #EXTM3U
            #EXT-X-TARGETDURATION:10
            #EXTINF:10.0,
            $name-0.ts
            #EXTINF:10.0,
            $name-1.ts
            #EXT-X-ENDLIST
        """.trimIndent()
        return HlsLoadedVariant(
            HlsVariant(0, 1, "$name.m3u8", mapOf("BANDWIDTH" to "$bandwidth", "RESOLUTION" to "1x$height")),
            "https://example.com/$name.m3u8".toHttpUrl(),
            parseHlsMediaPlaylist(playlist),
        )
    }

    @Test
    fun `a resumed segment keeps the variant it started on, even when Auto would now choose another`() {
        val set = HlsVariantSet(0, listOf(variant(500_000, 360, "low"), variant(3_000_000, 1080, "high")))

        // A fast network: the first segment starts on the high variant.
        set.autoVariant(0, continues = false, bitrateEstimate = 20_000_000) shouldBe 1
        // The connection drops mid-segment and the network is slow now. mpv asks for the rest of the segment: its
        // bytes must still be the high variant's, or mpv gets the start of one file and the end of another.
        set.autoVariant(0, continues = true, bitrateEstimate = 300_000) shouldBe 1
        // The next segment is chosen afresh.
        set.autoVariant(1, continues = false, bitrateEstimate = 300_000) shouldBe 0
    }

    private fun HlsVariantSet.autoVariant(index: Int, continues: Boolean, bitrateEstimate: Long): Int =
        variantForSegment(index, continues, HlsVariantPick.Auto, bitrateEstimate, bufferedAheadMs = 0)
}
