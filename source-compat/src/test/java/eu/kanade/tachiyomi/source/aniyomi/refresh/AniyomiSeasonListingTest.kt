package eu.kanade.tachiyomi.source.aniyomi.refresh

import eu.kanade.tachiyomi.animesource.model.FetchType
import eu.kanade.tachiyomi.animesource.model.SAnime
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class AniyomiSeasonListingTest {

    @Test
    fun `numbered seasons listed newest first come out oldest first, and unnumbered box set movies keep their order`() {
        // FilmiFen sorts its seasons by number, descending
        val filmiFen = listOf(season("/s3", number = 3.0), season("/s2", number = 2.0), season("/s1", number = 1.0))
        // A Jellyfin box set lists its movies by sort name and gives them no season number
        val boxSet = listOf(season("/2049", title = "Blade Runner 2049"), season("/1982", title = "Blade Runner"))

        oldestFirst(filmiFen).map { it.url } shouldBe listOf("/s1", "/s2", "/s3")
        oldestFirst(boxSet).map { it.url } shouldBe listOf("/2049", "/1982")
    }

    @Test
    fun `a series in a box set lists its seasons in its place, and a box set listing itself still ends`() = runTest {
        val boxSet = season("/box", seasons = true)
        val listings = mapOf(
            "/box" to listOf(
                season("/movie"),
                season("/series", seasons = true),
                boxSet,
            ),
            "/series" to listOf(season("/series/s2", number = 2.0), season("/series/s1", number = 1.0)),
        )

        val seasons = listSeasons(boxSet) { listings.getValue(it.url) }

        seasons.map { it.url } shouldBe listOf("/movie", "/series/s1", "/series/s2")
    }

    private fun season(
        url: String,
        title: String = url,
        number: Double = -1.0,
        seasons: Boolean = false,
    ): SAnime = SAnime.create().apply {
        this.url = url
        this.title = title
        season_number = number
        fetch_type = if (seasons) FetchType.Seasons else FetchType.Episodes
    }
}
