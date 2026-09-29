package mihon.entry.interactions.anime.download

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

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
}
