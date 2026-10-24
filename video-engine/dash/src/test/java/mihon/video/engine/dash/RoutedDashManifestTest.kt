package mihon.video.engine.dash

import io.kotest.matchers.shouldBe
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test

class RoutedDashManifestTest {

    @Test
    fun `after a redirect, relative references resolve against where the manifest is, through the proxy`() {
        val routed = routeDashManifest(
            $$"""
            <MPD xmlns="urn:mpeg:dash:schema:mpd:2011" type="static" mediaPresentationDuration="PT8S">
              <Period>
                <BaseURL>dash/</BaseURL>
                <AdaptationSet mimeType="video/mp4">
                  <SegmentTemplate media="$Number$.m4s" initialization="https://keys.example.org/init.mp4" duration="4"/>
                  <Representation id="v" bandwidth="1"><BaseURL>v1/</BaseURL></Representation>
                </AdaptationSet>
              </Period>
            </MPD>
            """.trimIndent(),
            // Asked for at origin.example.org, served from here after a redirect.
            "https://cdn2.example.org/vod/title/manifest.mpd".toHttpUrl(),
        ) { url -> "http://127.0.0.1:9/r/${url.host}${url.encodedPath}" }

        // Read the way mpv reads it: from the proxy URL of the address it asked for.
        val segments = readDashManifest(routed, "http://127.0.0.1:9/r/origin.example.org/manifest.mpd".toHttpUrl())
            .periods.single().adaptationSets.single().representations.single().segments()

        segments.media.map { it.segment.url.toString() } shouldBe listOf(
            "http://127.0.0.1:9/r/cdn2.example.org/vod/title/dash/v1/1.m4s",
            "http://127.0.0.1:9/r/cdn2.example.org/vod/title/dash/v1/2.m4s",
        )
        segments.initialization?.url.toString() shouldBe "http://127.0.0.1:9/r/keys.example.org/init.mp4"
    }
}
