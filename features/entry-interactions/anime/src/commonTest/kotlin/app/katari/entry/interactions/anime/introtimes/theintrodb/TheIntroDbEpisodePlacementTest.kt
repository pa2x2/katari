package app.katari.entry.interactions.anime.introtimes.theintrodb

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.TheIntroDbPlacement

class TheIntroDbEpisodePlacementTest {

    // The lengths of Attack on Titan's first three seasons on TVmaze, 2026-11-11.
    private val seasons = aired(1 to 25, 2 to 12, 3 to 22)

    @Test
    fun `an episode numbered past its season's end is asked for in the season it falls in`() {
        val fromTheStart = TheIntroDbPlacement(season = 1, firstEpisode = 1)

        episodeAsks(fromTheStart, number = 30, seasons) shouldBe listOf(TheIntroDbEpisode.InSeason(2, 5))
        episodeAsks(fromTheStart, number = 60, seasons) shouldBe emptyList()
        // A pick of season 3's first episode for the scope's thirtieth places the scope back in season 1.
        placementAt(index = 37, number = 30, seasons) shouldBe TheIntroDbPlacement(season = 1, firstEpisode = 9)
    }

    @Test
    fun `without a list of the series an episode is asked for as numbered, and by its running number in season 1`() {
        episodeAsks(TheIntroDbPlacement(season = 1, firstEpisode = 1), number = 30, emptyList()) shouldBe
            listOf(TheIntroDbEpisode.InSeason(1, 30), TheIntroDbEpisode.Running(30))
        episodeAsks(TheIntroDbPlacement(season = 2, firstEpisode = 1), number = 5, emptyList()) shouldBe
            listOf(TheIntroDbEpisode.InSeason(2, 5))
    }

    @Test
    fun `a series whose seasons are years is asked for by running number first`() {
        // TVmaze numbers One Piece's seasons by year, where TheIntroDB goes by TVDB's story seasons.
        val byYear = aired(1999 to 8, 2000 to 51)

        episodeAsks(TheIntroDbPlacement(season = 1999, firstEpisode = 1), number = 10, byYear) shouldBe
            listOf(TheIntroDbEpisode.Running(10), TheIntroDbEpisode.InSeason(2000, 2))
    }

    private fun aired(vararg seasons: Pair<Int, Int>): List<AiredEpisode> =
        seasons.flatMap { (season, count) ->
            (1..count).map { AiredEpisode(season = season, number = it, name = "", aired = null) }
        }
}
