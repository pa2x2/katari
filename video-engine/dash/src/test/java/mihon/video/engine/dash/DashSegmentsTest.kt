package mihon.video.engine.dash

import io.kotest.matchers.shouldBe
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test

// Manifests trimmed from the ones their hosts served (2026-10-24): DASH-IF's bbb_30fps on Akamai, Unified Streaming's
// tears-of-steel, the DASH-IF test case 1a.
class DashSegmentsTest {

    @Test
    fun `a duration template covers the period, its last segment partial`() {
        val manifest = readDashManifest(
            $$"""
            <MPD mediaPresentationDuration="PT634.566S" type="static" xmlns="urn:mpeg:dash:schema:mpd:2011">
             <BaseURL>./</BaseURL>
             <Period>
              <AdaptationSet mimeType="audio/mp4" contentType="audio">
               <SegmentTemplate duration="192512" timescale="48000" media="$RepresentationID$/$RepresentationID$_$Number$.m4a" startNumber="1" initialization="$RepresentationID$/$RepresentationID$_0.m4a"/>
               <Representation id="bbb_a64k" codecs="mp4a.40.5" bandwidth="67071" audioSamplingRate="48000"/>
              </AdaptationSet>
             </Period>
            </MPD>
            """.trimIndent(),
            "https://dash.akamaized.net/akamai/bbb_30fps/bbb_30fps.mpd".toHttpUrl(),
        )

        val segments = manifest.periods.single().adaptationSets.single().representations.single().segments()

        // 634.566 s in 4.0107 s segments: 158 whole ones and a shorter 159th.
        segments.media.size shouldBe 159
        segments.media.last().segment.url.toString() shouldBe
            "https://dash.akamaized.net/akamai/bbb_30fps/bbb_a64k/bbb_a64k_159.m4a"
        segments.initialization?.url.toString() shouldBe
            "https://dash.akamaized.net/akamai/bbb_30fps/bbb_a64k/bbb_a64k_0.m4a"
    }

    @Test
    fun `timeline segments are named by their times, an open-ended entry repeating to the period's end`() {
        val manifest = readDashManifest(
            $$"""
            <?xml version="1.0" encoding="utf-8"?>
            <MPD xmlns="urn:mpeg:dash:schema:mpd:2011" type="static" mediaPresentationDuration="PT12M14S">
              <Period id="1" duration="PT12M14S">
                <BaseURL>dash/</BaseURL>
                <AdaptationSet id="2" contentType="video" mimeType="video/mp4">
                  <SegmentTemplate timescale="600" initialization="tears-of-steel-$RepresentationID$.dash" media="tears-of-steel-$RepresentationID$-$Time$.dash">
                    <SegmentTimeline>
                      <S t="0" d="2400" r="182" />
                      <S d="1200" />
                    </SegmentTimeline>
                  </SegmentTemplate>
                  <Representation id="video_eng=401000" bandwidth="401000" width="224" height="100" codecs="avc1.42C00D"/>
                </AdaptationSet>
                <AdaptationSet id="3" contentType="audio" mimeType="audio/mp4">
                  <SegmentTemplate timescale="1000" startNumber="5" media="a-$Number%03d$.m4s">
                    <SegmentTimeline>
                      <S t="0" d="300000" />
                      <S d="4000" r="-1" />
                    </SegmentTimeline>
                  </SegmentTemplate>
                  <Representation id="a" bandwidth="64000"/>
                </AdaptationSet>
              </Period>
            </MPD>
            """.trimIndent(),
            // Where the manifest ended up after a redirect, not where it was asked for.
            "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.mpd"
                .toHttpUrl(),
        )
        val (video, audio) = manifest.periods.single().adaptationSets.map { it.representations.single().segments() }

        video.media.size shouldBe 184
        video.media.last().segment.url.toString() shouldBe
            "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/dash/" +
            "tears-of-steel-video_eng=401000-439200.dash"
        // 734 s: one 300 s segment, then 4 s ones up to the end, the last one partial.
        audio.media.size shouldBe 1 + 109
        audio.media.map { it.number }.let { it.first() to it.last() } shouldBe (5L to 114L)
        audio.media.last().segment.url.encodedPath shouldBe
            "/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/dash/a-114.m4s"
        audio.initialization shouldBe null
    }

    @Test
    fun `segment lists keep their byte ranges and a bare BaseURL is the whole file`() {
        val manifest = readDashManifest(
            """
            <MPD xmlns="urn:mpeg:dash:schema:mpd:2011" type="static" mediaPresentationDuration="PT654S">
              <BaseURL>http://dash.edgesuite.net/dash264/TestCases/1a/netflix/</BaseURL>
              <Period>
                <AdaptationSet mimeType="audio/mp4" codecs="mp4a.40.5" lang="en">
                  <Representation id="1" bandwidth="72000">
                    <BaseURL>ElephantsDream_AAC48K_064.mp4.dash</BaseURL>
                    <SegmentBase indexRange="871-11131"><Initialization range="0-870"/></SegmentBase>
                  </Representation>
                </AdaptationSet>
                <AdaptationSet mimeType="video/mp4" contentType="video">
                  <Representation id="v" bandwidth="150000" height="360">
                    <BaseURL>http://127.0.0.1:41000/v/stream</BaseURL>
                    <SegmentList timescale="1000" duration="6000">
                      <Initialization sourceURL="init" range="0-99"/>
                      <SegmentURL media="seg?i=1"/>
                      <SegmentURL mediaRange="100-199"/>
                    </SegmentList>
                  </Representation>
                </AdaptationSet>
              </Period>
            </MPD>
            """.trimIndent(),
            "https://example.org/manifest.mpd".toHttpUrl(),
        )
        val (audio, video) = manifest.periods.single().adaptationSets.map { it.representations.single().segments() }

        audio.initialization shouldBe null
        audio.media.single().segment.let { it.url.toString() to it.range } shouldBe
            ("http://dash.edgesuite.net/dash264/TestCases/1a/netflix/ElephantsDream_AAC48K_064.mp4.dash" to null)
        video.initialization?.let { it.url.toString() to it.range } shouldBe
            ("http://127.0.0.1:41000/v/init" to 0L..99L)
        video.media.map { it.segment.url.toString() to it.segment.range } shouldBe listOf(
            "http://127.0.0.1:41000/v/seg?i=1" to null,
            "http://127.0.0.1:41000/v/stream" to 100L..199L,
        )
    }
}
