package mihon.entry.interactions.anime.download

import io.kotest.matchers.shouldBe
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class AnimeDownloaderTest {

    @Test
    fun `only variant and rendition playlist uris are nested playlists`() {
        isHlsPlaylistReference(
            url = "https://cdn.example.com/720p",
            previousTagLine = "#EXT-X-STREAM-INF:BANDWIDTH=1920000,RESOLUTION=1280x720",
        ) shouldBe true
        isHlsPlaylistReference(
            url = "https://cdn.example.com/audio/main",
            currentTagLine = "#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"audio\",URI=\"audio/main\"",
        ) shouldBe true
        isHlsPlaylistReference(
            url = "https://cdn.example.com/subtitles/ar.vtt",
            currentTagLine = "#EXT-X-MEDIA:TYPE=SUBTITLES,GROUP-ID=\"subs\",URI=\"subtitles/ar.vtt\"",
        ) shouldBe false
        isHlsPlaylistReference(
            url = "https://cdn.example.com/key",
            currentTagLine = "#EXT-X-KEY:METHOD=AES-128,URI=\"key\"",
        ) shouldBe false
    }

    @Test
    fun `rewrites staged playlist references to actual SAF filenames`() {
        rewriteHlsPlaylistReferences(
            playlistText = """
                |#EXTM3U
                |#EXT-X-STREAM-INF:BANDWIDTH=2253013
                |958c73d1_index-v1-a1.m3u8
            """.trimMargin(),
            stagedToActualNames = mapOf(
                "video.m3u8" to "video.m3u",
                "958c73d1_index-v1-a1.m3u8" to "958c73d1_index-v1-a1.m3u",
            ),
        ) shouldBe """
            |#EXTM3U
            |#EXT-X-STREAM-INF:BANDWIDTH=2253013
            |958c73d1_index-v1-a1.m3u
        """.trimMargin()
    }

    @Test
    fun `file transfer client disables total call timeout`() {
        val baseClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(2, TimeUnit.MINUTES)
            .build()

        val fileTransferClient = createFileTransferClient(baseClient)

        fileTransferClient.callTimeoutMillis shouldBe 0
        fileTransferClient.connectTimeoutMillis shouldBe baseClient.connectTimeoutMillis
        fileTransferClient.readTimeoutMillis shouldBe baseClient.readTimeoutMillis
    }
}
