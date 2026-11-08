package eu.kanade.tachiyomi.data.track.url

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TrackerUrlBuilderTest {

    // The expected address is what Android's Uri.Builder made of the same calls, which trackers have always received.
    // OkHttp's HttpUrl, for one, would send "%28Official%29%21", "%7E" and "%27" and leave the ":" in the path.
    @Test
    fun `titles and IDs are encoded as Android's Uri builder encodes them`() {
        val url = TrackerUrlBuilder("https://api.myanimelist.net/v2/manga")
            .appendPath("進撃:1")
            .appendQueryParameter("q", "Love Is War (Official)! ~ 'Tis 100%")
            .appendQueryParameter("fields", "num_chapters,my_list_status{start_date}")
            .build()

        url shouldBe "https://api.myanimelist.net/v2/manga/%E9%80%B2%E6%92%83%3A1" +
            "?q=Love%20Is%20War%20(Official)!%20~%20'Tis%20100%25" +
            "&fields=num_chapters%2Cmy_list_status%7Bstart_date%7D"
    }
}
