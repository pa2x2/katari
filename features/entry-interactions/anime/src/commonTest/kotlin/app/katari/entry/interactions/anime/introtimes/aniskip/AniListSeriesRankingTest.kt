package app.katari.entry.interactions.anime.introtimes.aniskip

import app.katari.entry.interactions.anime.introtimes.IntroTimesScope
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AniListSeriesRankingTest {

    @Test
    fun `a Russian title finds the season AniList names in another word order, not the next season ranked first`() {
        // AniList's order and names for this search, 2026-10-26.
        val secondSeason = series(
            malId = 59978,
            title = "Sousou no Frieren 2nd Season",
            year = 2026,
            episodes = 10,
            "Провожающая в последний путь Фрирен 2",
        )
        val firstSeason =
            series(52991, "Sousou no Frieren", 2023, episodes = 28, "Фрирен, провожающая в последний путь")

        bestSeries(listOf(secondSeason, firstSeason), scope("Провожающая в последний путь Фрирен", 2023, 28)) shouldBe
            firstSeason
    }

    @Test
    fun `a remake with the same name is told apart by year, or by episode count when the source gives no dates`() {
        val original = series(136, "HUNTER×HUNTER", 1999, episodes = 62, "Hunter x Hunter")
        val remake = series(11061, "HUNTER×HUNTER (2011)", 2011, episodes = 148, "Hunter x Hunter (2011)")
        val candidates = listOf(original, remake)

        bestSeries(candidates, scope("Hunter x Hunter", year = null, episodeCount = 148)) shouldBe remake
        bestSeries(candidates, scope("Hunter x Hunter", year = 1999, episodeCount = 62)) shouldBe original
        bestSeries(candidates, scope("Hunter x Hunter Movie", year = null, episodeCount = 1)) shouldBe null
    }

    @Test
    fun `a later season is the one named by its number, or the one that aired with the episodes when names differ`() {
        // AniList's seasons of Attack on Titan in the order they aired, 2026-11-11.
        val first = series(16498, "Shingeki no Kyojin", 2013, episodes = 25, "Attack on Titan")
        val second = series(25777, "Shingeki no Kyojin Season 2", 2017, episodes = 12, "Attack on Titan Season 2")
        val third = series(35760, "Shingeki no Kyojin Season 3", 2018, episodes = 12, "Attack on Titan Season 3")
        val thirdSecondPart =
            series(38524, "Shingeki no Kyojin Season 3 Part 2", 2019, episodes = 10, "Attack on Titan Season 3 Part 2")
        val final =
            series(40028, "Shingeki no Kyojin: The Final Season", 2020, episodes = 16, "Attack on Titan Final Season")
        val seasons = listOf(first, second, third, thirdSecondPart, final)

        seasonSeries(seasons, season = 3, scope("Attack on Titan", year = null, episodeCount = 22)) shouldBe third
        // The fourth season is the fifth series: the third season's second part comes between.
        seasonSeries(seasons, season = 4, scope("Attack on Titan", year = 2020, episodeCount = 16)) shouldBe final
        seasonSeries(seasons, season = 4, scope("Attack on Titan", year = null, episodeCount = 16)) shouldBe null
    }

    private fun series(malId: Long, title: String, year: Int, episodes: Int, vararg names: String) = AniListSeries(
        anilistId = malId + 100_000,
        malId = malId,
        title = title,
        titles = listOf(title) + names,
        year = year,
        startOrder = year * 10_000,
        format = "TV",
        episodes = episodes,
        airedEpisodes = null,
        coverUrl = null,
        episodeNames = emptyMap(),
        prequels = emptyList(),
        sequels = emptyList(),
    )

    private fun scope(title: String, year: Int?, episodeCount: Int) =
        IntroTimesScope(
            entryId = 1,
            groupKey = null,
            title = title,
            seasonName = null,
            listedSeason = null,
            year = year,
            episodeCount = episodeCount,
            firstNumber = 1,
            lastNumber = episodeCount,
            firstAired = null,
            lastAired = null,
        )
}
