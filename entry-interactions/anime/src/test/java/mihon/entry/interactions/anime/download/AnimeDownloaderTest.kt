package mihon.entry.interactions.anime.download

import eu.kanade.tachiyomi.source.entry.VideoPlaybackOption
import io.kotest.matchers.shouldBe
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.VideoDownloadQualityMode
import java.util.concurrent.TimeUnit

class AnimeDownloaderTest {

    @Test
    fun `treats extensionless stream variant as nested playlist`() {
        isHlsPlaylistReference(
            url = "https://cdn.example.com/720p",
            previousTagLine = "#EXT-X-STREAM-INF:BANDWIDTH=1920000,RESOLUTION=1280x720",
        ) shouldBe true
    }

    @Test
    fun `treats media uri as nested playlist even without extension`() {
        isHlsPlaylistReference(
            url = "https://cdn.example.com/audio/main",
            currentTagLine = "#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"audio\",URI=\"audio/main\"",
        ) shouldBe true
    }

    @Test
    fun `does not treat subtitle asset uri as nested playlist`() {
        isHlsPlaylistReference(
            url = "https://cdn.example.com/subtitles/ar.vtt",
            currentTagLine = "#EXT-X-MEDIA:TYPE=SUBTITLES,GROUP-ID=\"subs\",URI=\"subtitles/ar.vtt\"",
        ) shouldBe false
    }

    @Test
    fun `does not treat key uri as nested playlist`() {
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
    fun `best quality selects highest source quality option`() {
        selectSourceQualityForPreferences(
            qualityMode = VideoDownloadQualityMode.BEST,
            sourceQualities = listOf(
                VideoPlaybackOption(key = "240", label = "240p"),
                VideoPlaybackOption(key = "720", label = "720p"),
                VideoPlaybackOption(key = "1080", label = "1080p"),
            ),
        ) shouldBe "1080"
    }

    @Test
    fun `data saving selects lowest source quality option`() {
        selectSourceQualityForPreferences(
            qualityMode = VideoDownloadQualityMode.DATA_SAVING,
            sourceQualities = listOf(
                VideoPlaybackOption(key = "240", label = "240p"),
                VideoPlaybackOption(key = "720", label = "720p"),
                VideoPlaybackOption(key = "1080", label = "1080p"),
            ),
        ) shouldBe "240"
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
