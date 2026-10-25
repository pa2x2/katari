package mihon.video.engine.mpv.proxy.hls

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test

class HlsVariantSetsTest {

    // test-streams.mux.dev/x36xhzz: three AAC-LC variants and two HE-AAC ones, with their last segments, whose
    // durations each variant rounds to its own frames.
    private val muxMaster = """
        #EXTM3U
        #EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=2149280,CODECS="mp4a.40.2,avc1.64001f",RESOLUTION=1280x720,NAME="720"
        url_0/193039199_mp4_h264_aac_hd_7.m3u8
        #EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=246440,CODECS="mp4a.40.5,avc1.42000d",RESOLUTION=320x184,NAME="240"
        url_2/193039199_mp4_h264_aac_ld_7.m3u8
        #EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=460560,CODECS="mp4a.40.5,avc1.420016",RESOLUTION=512x288,NAME="380"
        url_4/193039199_mp4_h264_aac_7.m3u8
        #EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=836280,CODECS="mp4a.40.2,avc1.64001f",RESOLUTION=848x480,NAME="480"
        url_6/193039199_mp4_h264_aac_hq_7.m3u8
        #EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=6221600,CODECS="mp4a.40.2,avc1.640028",RESOLUTION=1920x1080,NAME="1080"
        url_8/193039199_mp4_h264_aac_fhd_7.m3u8
    """.trimIndent()

    private fun muxPlaylist(firstSegment: Int, file: String, lastDuration: String) = """
        #EXTM3U
        #EXT-X-VERSION:3
        #EXT-X-PLAYLIST-TYPE:VOD
        #EXT-X-TARGETDURATION:11
        #EXTINF:10.000,
        url_$firstSegment/193039199_mp4_h264_$file.ts
        #EXTINF:10.000,
        url_${firstSegment + 1}/193039199_mp4_h264_$file.ts
        #EXTINF:$lastDuration,
        url_${firstSegment + 2}/193039199_mp4_h264_$file.ts
        #EXT-X-ENDLIST
    """.trimIndent()

    private val muxPlaylists = listOf(
        muxPlaylist(523, "aac_hd_7", "4.584"),
        muxPlaylist(587, "aac_ld_7", "4.634"),
        muxPlaylist(779, "aac_7", "4.634"),
        muxPlaylist(907, "aac_hq_7", "4.600"),
        muxPlaylist(651, "aac_fhd_7", "4.567"),
    )

    private fun loaded(masterText: String, playlists: List<String>): Pair<HlsMasterPlaylist, List<HlsLoadedVariant>> {
        val base = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8".toHttpUrl()
        val master = parseHlsMasterPlaylist(masterText)!!
        return master to master.variants.zip(playlists) { variant, playlist ->
            HlsLoadedVariant(variant, base.resolve(variant.uri)!!, parseHlsMediaPlaylist(playlist))
        }
    }

    @Test
    fun `only variants with the same audio codec switch together, and each set keeps the number ffmpeg gives it`() {
        val (master, variants) = loaded(muxMaster, muxPlaylists)

        val sets = findHlsVariantSets(variants)
        val (text, programIds) = hlsMasterWithVariantSets(master, sets, { "set-$it" }) { it }

        // AAC-LC and HE-AAC don't mix: mpv misreads the audio timestamps after a change between them.
        sets.map { set -> set.map { it.variant.height } } shouldBe listOf(listOf(480, 720, 1080), listOf(184, 288))
        // ffmpeg numbers the variants left in the master playlist in order: the 240p one, then the 480p one.
        programIds shouldBe listOf(1, 0)
        text.lines().filter { it.startsWith("set-") } shouldBe listOf("set-1", "set-0")
    }

    @Test
    fun `fMP4 variants are left to mpv, as ffmpeg reads only the first initialization section`() {
        val master = """
            #EXTM3U
            #EXT-X-STREAM-INF:BANDWIDTH=6096050,CODECS="avc1.4d401f,mp4a.40.2",RESOLUTION=640x480
            playlist_v-0480p-1000k-libx264.mp4.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=4005148,CODECS="avc1.4d401f,mp4a.40.2",RESOLUTION=480x360
            playlist_v-0360p-0750k-libx264.mp4.m3u8
        """.trimIndent()
        fun playlist(name: String) = """
            #EXTM3U
            #EXT-X-VERSION:6
            #EXT-X-TARGETDURATION:5
            #EXT-X-PLAYLIST-TYPE:VOD
            #EXT-X-MAP:URI="v-$name-init.mp4"
            #EXTINF:4.000,
            v-$name-s1.mp4
            #EXTINF:4.000,
            v-$name-s2.mp4
            #EXT-X-ENDLIST
        """.trimIndent()

        val (_, variants) = loaded(master, listOf(playlist("0480p-1000k-libx264"), playlist("0360p-0750k-libx264")))

        findHlsVariantSets(variants).shouldBeEmpty()
    }
}
