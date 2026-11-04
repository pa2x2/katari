package mihon.entry.interactions.anime.download.stream.hls

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.anime.download.stream.RemoteText
import mihon.entry.interactions.anime.stream.PreferredHeight
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test

class HlsDownloadPlanTest {

    @Test
    fun `a multivariant download keeps one variant and the renditions of its groups, not every quality`() = runTest {
        val fetched = mutableListOf<String>()
        val plan = planHlsDownload(
            root = RemoteText(APPLE_MASTER, "$APPLE/master.m3u8".toHttpUrl()),
            preferredHeight = PreferredHeight.Near(720),
            isDecodable = { codecs -> "ac-3" !in codecs.orEmpty() },
            rootName = "video",
            rootFileName = "video.m3u",
        ) { url ->
            fetched += url.toString()
            RemoteText("#EXTM3U\n#EXTINF:6.0,\nseg.mp4\n#EXT-X-ENDLIST\n", url)
        }

        fetched shouldBe
            listOf("$APPLE/v6/prog_index.m3u8", "$APPLE/a1/prog_index.m3u8", "$APPLE/s1/en/prog_index.m3u8")
        plan.texts["video.m3u"] shouldBe """
            #EXTM3U
            #EXT-X-VERSION:6
            #EXT-X-INDEPENDENT-SEGMENTS
            #EXT-X-STREAM-INF:AVERAGE-BANDWIDTH=3168702,BANDWIDTH=3216424,CODECS="avc1.640020,mp4a.40.2",RESOLUTION=1280x720,FRAME-RATE=60.000,CLOSED-CAPTIONS="cc1",AUDIO="aud1",SUBTITLES="sub1"
            video-v.m3u
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="aud1",LANGUAGE="en",NAME="English",AUTOSELECT=YES,DEFAULT=YES,CHANNELS="2",URI="video-a1.m3u"
            #EXT-X-MEDIA:TYPE=CLOSED-CAPTIONS,GROUP-ID="cc1",LANGUAGE="en",NAME="English",AUTOSELECT=YES,DEFAULT=YES,INSTREAM-ID="CC1"
            #EXT-X-MEDIA:TYPE=SUBTITLES,GROUP-ID="sub1",LANGUAGE="en",NAME="English",AUTOSELECT=YES,DEFAULT=YES,FORCED=NO,URI="video-s1.m3u"

        """.trimIndent()
        plan.files.map { it.name } shouldBe listOf("video-v-1.mp4", "video-a1-1.mp4", "video-s1-1.mp4")
    }

    @Test
    fun `byte-range segments become files of their own, a range without an offset following the previous one`() {
        val playlist = localHlsMediaPlaylist(
            text = """
                #EXTM3U
                #EXT-X-VERSION:7
                #EXT-X-MAP:URI="main.mp4",BYTERANGE="720@0"
                #EXTINF:6.00000,
                #EXT-X-BYTERANGE:2267397@720
                main.mp4
                #EXTINF:6.00000,
                #EXT-X-BYTERANGE:2258821
                main.mp4
                #EXT-X-ENDLIST
            """.trimIndent(),
            url = "$APPLE/v6/prog_index.m3u8".toHttpUrl(),
            prefix = "video-v",
        )

        playlist.text shouldBe """
            #EXTM3U
            #EXT-X-VERSION:7
            #EXT-X-MAP:URI="video-v-init1.mp4"
            #EXTINF:6.00000,
            video-v-1.mp4
            #EXTINF:6.00000,
            video-v-2.mp4
            #EXT-X-ENDLIST

        """.trimIndent()
        val main: HttpUrl = "$APPLE/v6/main.mp4".toHttpUrl()
        playlist.files.map { Triple(it.name, it.url, it.range) } shouldBe listOf(
            Triple("video-v-init1.mp4", main, 0L..719L),
            Triple("video-v-1.mp4", main, 720L..2_268_116L),
            Triple("video-v-2.mp4", main, 2_268_117L..4_526_937L),
        )
    }

    @Test
    fun `a retry matches segments by place, so fresh links resume but changed timing starts over`() = runTest {
        suspend fun plan(token: String, lastDuration: String) = planHlsDownload(
            root = RemoteText(
                "#EXTM3U\n#EXT-X-STREAM-INF:BANDWIDTH=900000,RESOLUTION=640x360\nhttps://cdn.example/$token/v.m3u8\n",
                "https://cdn.example/$token/master.m3u8".toHttpUrl(),
            ),
            preferredHeight = PreferredHeight.Near(720),
            isDecodable = { true },
            rootName = "video",
            rootFileName = "video.m3u",
        ) { url ->
            val text = "#EXTM3U\n#EXTINF:10.0,\nseg-1.ts?sig=$token\n#EXTINF:$lastDuration,\nseg-2.ts?sig=$token\n"
            RemoteText(text, url)
        }

        plan("aaa", lastDuration = "4.2").id shouldBe plan("bbb", lastDuration = "4.2").id
        (plan("aaa", lastDuration = "4.2").id == plan("bbb", lastDuration = "5.0").id) shouldBe false
    }

    private companion object {
        const val APPLE = "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4"

        // Apple's advanced example, cut to three qualities per audio group.
        val APPLE_MASTER = """
            #EXTM3U
            #EXT-X-VERSION:6
            #EXT-X-INDEPENDENT-SEGMENTS


            #EXT-X-STREAM-INF:AVERAGE-BANDWIDTH=2168183,BANDWIDTH=2177116,CODECS="avc1.640020,mp4a.40.2",RESOLUTION=960x540,FRAME-RATE=60.000,CLOSED-CAPTIONS="cc1",AUDIO="aud1",SUBTITLES="sub1"
            v5/prog_index.m3u8
            #EXT-X-STREAM-INF:AVERAGE-BANDWIDTH=7968416,BANDWIDTH=8001098,CODECS="avc1.64002a,mp4a.40.2",RESOLUTION=1920x1080,FRAME-RATE=60.000,CLOSED-CAPTIONS="cc1",AUDIO="aud1",SUBTITLES="sub1"
            v9/prog_index.m3u8
            #EXT-X-STREAM-INF:AVERAGE-BANDWIDTH=3168702,BANDWIDTH=3216424,CODECS="avc1.640020,mp4a.40.2",RESOLUTION=1280x720,FRAME-RATE=60.000,CLOSED-CAPTIONS="cc1",AUDIO="aud1",SUBTITLES="sub1"
            v6/prog_index.m3u8


            #EXT-X-STREAM-INF:AVERAGE-BANDWIDTH=2390686,BANDWIDTH=2399619,CODECS="avc1.640020,ac-3",RESOLUTION=960x540,FRAME-RATE=60.000,CLOSED-CAPTIONS="cc1",AUDIO="aud2",SUBTITLES="sub1"
            v5/prog_index.m3u8
            #EXT-X-STREAM-INF:AVERAGE-BANDWIDTH=8190919,BANDWIDTH=8223601,CODECS="avc1.64002a,ac-3",RESOLUTION=1920x1080,FRAME-RATE=60.000,CLOSED-CAPTIONS="cc1",AUDIO="aud2",SUBTITLES="sub1"
            v9/prog_index.m3u8
            #EXT-X-STREAM-INF:AVERAGE-BANDWIDTH=3391205,BANDWIDTH=3438927,CODECS="avc1.640020,ac-3",RESOLUTION=1280x720,FRAME-RATE=60.000,CLOSED-CAPTIONS="cc1",AUDIO="aud2",SUBTITLES="sub1"
            v6/prog_index.m3u8


            #EXT-X-I-FRAME-STREAM-INF:AVERAGE-BANDWIDTH=183689,BANDWIDTH=187492,CODECS="avc1.64002a",RESOLUTION=1920x1080,URI="v7/iframe_index.m3u8"
            #EXT-X-I-FRAME-STREAM-INF:AVERAGE-BANDWIDTH=132672,BANDWIDTH=136398,CODECS="avc1.640020",RESOLUTION=1280x720,URI="v6/iframe_index.m3u8"


            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="aud1",LANGUAGE="en",NAME="English",AUTOSELECT=YES,DEFAULT=YES,CHANNELS="2",URI="a1/prog_index.m3u8"
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="aud2",LANGUAGE="en",NAME="English",AUTOSELECT=YES,DEFAULT=YES,CHANNELS="6",URI="a2/prog_index.m3u8"


            #EXT-X-MEDIA:TYPE=CLOSED-CAPTIONS,GROUP-ID="cc1",LANGUAGE="en",NAME="English",AUTOSELECT=YES,DEFAULT=YES,INSTREAM-ID="CC1"


            #EXT-X-MEDIA:TYPE=SUBTITLES,GROUP-ID="sub1",LANGUAGE="en",NAME="English",AUTOSELECT=YES,DEFAULT=YES,FORCED=NO,URI="s1/en/prog_index.m3u8"
        """.trimIndent()
    }
}
