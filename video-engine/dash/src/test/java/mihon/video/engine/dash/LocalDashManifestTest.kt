package mihon.video.engine.dash

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test

class LocalDashManifestTest {

    // Trimmed from Unified Streaming's tears-of-steel and Shaka's angel-one (2026-10-24): the audio set's template is
    // inherited by its representations, and the text set is one file with an index.
    @Test
    fun `the local manifest names exactly the downloaded files and nothing on the network`() {
        val remote = readDashManifest(
            $$"""
            <?xml version="1.0" encoding="utf-8"?>
            <MPD xmlns="urn:mpeg:dash:schema:mpd:2011" type="static" mediaPresentationDuration="PT12S">
              <Location>https://cdn.example.org/live/refresh.mpd</Location>
              <Period id="1" duration="PT12S">
                <BaseURL>dash/</BaseURL>
                <AdaptationSet id="1" contentType="audio" lang="en" mimeType="audio/mp4" codecs="mp4a.40.2">
                  <Role schemeIdUri="urn:mpeg:dash:role:2011" value="main" />
                  <SegmentTemplate timescale="48000" initialization="tos-$RepresentationID$.dash" media="tos-$RepresentationID$-$Time$.dash">
                    <SegmentTimeline><S t="0" d="192000" r="2" /></SegmentTimeline>
                  </SegmentTemplate>
                  <Representation id="audio_eng=64008" bandwidth="64008" />
                  <Representation id="audio_eng=128002" bandwidth="128002" />
                </AdaptationSet>
                <AdaptationSet id="2" contentType="text" lang="el">
                  <Representation id="0" bandwidth="1044" codecs="wvtt" mimeType="application/mp4">
                    <BaseURL>text_el.mp4</BaseURL>
                    <SegmentBase indexRange="727-950" timescale="1000"><Initialization range="0-726"/></SegmentBase>
                  </Representation>
                </AdaptationSet>
              </Period>
            </MPD>
            """.trimIndent(),
            "https://cdn.example.org/tos/tos.ism/.mpd".toHttpUrl(),
        )
        val (audio, text) = remote.periods.single().adaptationSets
        val kept = mapOf(audio.representations[1] to "a1", text.representations.single() to "t1")

        val local = localDashManifest(remote, kept)

        local.files.map { it.name to it.segment.url.toString() } shouldBe listOf(
            "a1-init.mp4" to "https://cdn.example.org/tos/tos.ism/dash/tos-audio_eng=128002.dash",
            "a1-1.m4s" to "https://cdn.example.org/tos/tos.ism/dash/tos-audio_eng=128002-0.dash",
            "a1-2.m4s" to "https://cdn.example.org/tos/tos.ism/dash/tos-audio_eng=128002-192000.dash",
            "a1-3.m4s" to "https://cdn.example.org/tos/tos.ism/dash/tos-audio_eng=128002-384000.dash",
            "t1.mp4" to "https://cdn.example.org/tos/tos.ism/dash/text_el.mp4",
        )
        local.text shouldNotContain "<BaseURL>dash/"
        local.text shouldNotContain "Location"
        local.text shouldNotContain "64008"
        // The DASH default, written out: FFmpeg counts from 0 without it, so mpv asked for an "a1-0.m4s".
        local.text shouldContain "startNumber=\"1\""

        // Read back as a player reads it from the download folder, every reference must be one of those files.
        val offline = readDashManifest(local.text, "https://downloads.invalid/episode/video.mpd".toHttpUrl())
        val (offlineAudio, offlineText) = offline.periods.single().adaptationSets.map { it.representations.single() }
        val audioSegments = offlineAudio.segments()
        listOfNotNull(audioSegments.initialization).plus(audioSegments.media.map { it.segment })
            .map { it.url.pathSegments.last() } shouldBe listOf("a1-init.mp4", "a1-1.m4s", "a1-2.m4s", "a1-3.m4s")
        offline.periods.single().adaptationSets.first().language shouldBe "en"
        offlineText.segments().media.single().segment.url.pathSegments.last() shouldBe "t1.mp4"
        (offlineText.addressing as DashAddressing.SingleFile).segmentBase?.attr("indexRange") shouldBe "727-950"
    }
}
