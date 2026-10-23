package mihon.video.engine.mpv.proxy

import io.kotest.matchers.shouldBe
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test

class HlsPlaylistRewriterTest {

    private val routes = ProxyRoutes(port = 40123, token = "t0k3n")
    private val proxy = "http://127.0.0.1:40123/t0k3n/r/v"

    @Test
    fun `every reference, absolute, root-relative or relative, in a line or a URI attribute, goes through the proxy`() {
        val master = "https://video.example.org/hls/show/ep1/master.m3u8?token=abc".toHttpUrl()
        val media = "https://video.example.org/hls/show/ep1/1080/index.m3u8".toHttpUrl()

        rewrite(
            """
            #EXTM3U
            #EXT-X-VERSION:6
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="aud",NAME="Japanese",LANGUAGE="ja",DEFAULT=YES,URI="audio/ja/index.m3u8"
            #EXT-X-MEDIA:TYPE=SUBTITLES,GROUP-ID="sub",NAME="English",LANGUAGE="en",URI="https://subs.example.net/ep1/en.m3u8?sig=x%2Fy"
            #EXT-X-STREAM-INF:BANDWIDTH=5000000,RESOLUTION=1920x1080,AUDIO="aud",SUBTITLES="sub"
            1080/index.m3u8
            #EXT-X-I-FRAME-STREAM-INF:BANDWIDTH=200000,URI="/hls/show/ep1/1080/iframes.m3u8"
            """.trimIndent(),
            master,
        ) shouldBe """
            #EXTM3U
            #EXT-X-VERSION:6
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="aud",NAME="Japanese",LANGUAGE="ja",DEFAULT=YES,URI="$proxy/https/video.example.org/hls/show/ep1/audio/ja/index.m3u8"
            #EXT-X-MEDIA:TYPE=SUBTITLES,GROUP-ID="sub",NAME="English",LANGUAGE="en",URI="$proxy/https/subs.example.net/ep1/en.m3u8?sig=x%2Fy"
            #EXT-X-STREAM-INF:BANDWIDTH=5000000,RESOLUTION=1920x1080,AUDIO="aud",SUBTITLES="sub"
            $proxy/https/video.example.org/hls/show/ep1/1080/index.m3u8
            #EXT-X-I-FRAME-STREAM-INF:BANDWIDTH=200000,URI="$proxy/https/video.example.org/hls/show/ep1/1080/iframes.m3u8"
        """.trimIndent()

        rewrite(
            "#EXTM3U\r\n" +
                "#EXT-X-TARGETDURATION:6\r\n" +
                "#EXT-X-KEY:METHOD=AES-128,URI=\"/keys/ep1.key\",IV=0x1\r\n" +
                "#EXT-X-MAP:URI=\"init.mp4\"\r\n" +
                "#EXTINF:6.0,\r\n" +
                "seg-00001.m4s\r\n" +
                "#EXTINF:6.0,\r\n" +
                "https://cdn7.example.com:8443/v/ep1/seg-00002.m4s?expires=1700000000&hash=Zm9v\r\n" +
                "#EXT-X-KEY:METHOD=SAMPLE-AES,URI=\"data:text/plain;base64,AAAA\"\r\n" +
                "#EXT-X-ENDLIST\r\n",
            media,
        ) shouldBe "#EXTM3U\r\n" +
            "#EXT-X-TARGETDURATION:6\r\n" +
            "#EXT-X-KEY:METHOD=AES-128,URI=\"$proxy/https/video.example.org/keys/ep1.key\",IV=0x1\r\n" +
            "#EXT-X-MAP:URI=\"$proxy/https/video.example.org/hls/show/ep1/1080/init.mp4\"\r\n" +
            "#EXTINF:6.0,\r\n" +
            "$proxy/https/video.example.org/hls/show/ep1/1080/seg-00001.m4s\r\n" +
            "#EXTINF:6.0,\r\n" +
            "$proxy/https/cdn7.example.com:8443/v/ep1/seg-00002.m4s?expires=1700000000&hash=Zm9v\r\n" +
            "#EXT-X-KEY:METHOD=SAMPLE-AES,URI=\"data:text/plain;base64,AAAA\"\r\n" +
            "#EXT-X-ENDLIST\r\n"

        routes.parse(
            "/t0k3n/r/v/https/cdn7.example.com:8443/v/ep1/seg-00002.m4s",
            "expires=1700000000&hash=Zm9v",
        ) shouldBe
            ProxyTarget.Remote(
                HeaderSet.Stream,
                "https://cdn7.example.com:8443/v/ep1/seg-00002.m4s?expires=1700000000&hash=Zm9v".toHttpUrl(),
            )
    }

    private fun rewrite(playlist: String, base: okhttp3.HttpUrl): String =
        rewriteHlsPlaylist(playlist) { routes.remoteReference(HeaderSet.Stream, base, it) }
}
