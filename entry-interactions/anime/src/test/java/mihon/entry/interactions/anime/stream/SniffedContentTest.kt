package mihon.entry.interactions.anime.stream

import eu.kanade.tachiyomi.source.entry.VideoStreamType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SniffedContentTest {

    // Prefixes and Content-Types as real hosts served them (2026-10-24): mux's test HLS, Shaka's packager and Unified
    // Streaming's DASH, a progressive MP4 from test-videos.co.uk.
    @Test
    fun `playlists and manifests are recognised whatever their Content-Type, and an error page is never media`() {
        fun sniff(
            text: String,
            contentType: String?,
        ) = sniffStreamContent(text.toByteArray(Charsets.ISO_8859_1), contentType)

        // An HLS playlist served as octet-stream, behind a byte order mark, was taken for a progressive file.
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val bomPlaylist = bom + "#EXTM3U\n#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=2149280\n".toByteArray()
        sniffStreamContent(bomPlaylist, "application/octet-stream") shouldBe
            SniffedContent(VideoStreamType.HLS)
        sniff("#EXTM3U\n#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=2149280\n", "audio/mpegurl") shouldBe
            SniffedContent(VideoStreamType.HLS)

        // An MPD behind an XML declaration and a comment, served as octet-stream, wasn't recognised.
        sniff(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<!--Generated with https://github.com/google/shaka-packager -->\n" +
                "<MPD xmlns=\"urn:mpeg:dash:schema:mpd:2011\" type=\"static\">",
            "application/octet-stream",
        ) shouldBe SniffedContent(VideoStreamType.DASH)
        sniff(
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<!-- Created with Unified Streaming Platform  (version=1.13.0-29687) -->\n" +
                "<MPD\n  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"",
            "text/plain",
        ) shouldBe SniffedContent(VideoStreamType.DASH)

        // An anti-bot page claiming to be video counted as a progressive file.
        sniff("<!DOCTYPE html><html><head><title>Just a moment...</title>", "video/mp4") shouldBe null
        sniff("<?xml version='1.0' encoding='UTF-8'?><Error><Code>AccessDenied</Code>", "application/xml") shouldBe null

        // Containers by their signature, not by a Content-Type that says nothing.
        val mp4 = byteArrayOf(0, 0, 0, 0x20) + "ftypisom".toByteArray() + ByteArray(8)
        sniffStreamContent(mp4, "application/octet-stream") shouldBe
            SniffedContent(VideoStreamType.PROGRESSIVE, "video/mp4")
        val mkv = byteArrayOf(0x1A, 0x45, 0xDF.toByte(), 0xA3.toByte()) + "\u0042\u0082webm".toByteArray()
        sniffStreamContent(mkv, null) shouldBe SniffedContent(VideoStreamType.PROGRESSIVE, "video/webm")
        val ts = ByteArray(376).also {
            it[0] = 0x47
            it[188] = 0x47
        }
        sniffStreamContent(ts, "image/png") shouldBe SniffedContent(VideoStreamType.PROGRESSIVE, "video/mp2t")
    }
}
