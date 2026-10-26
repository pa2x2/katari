package mihon.entry.interactions.anime.introtimes

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class IntroTimesSeriesRankingTest {

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

    private fun series(malId: Long, title: String, year: Int, episodes: Int, vararg names: String) = AniListSeries(
        anilistId = malId + 100_000,
        malId = malId,
        title = title,
        titles = listOf(title) + names,
        year = year,
        format = "TV",
        episodes = episodes,
        coverUrl = null,
    )

    private fun scope(title: String, year: Int?, episodeCount: Int) =
        IntroTimesScope(entryId = 1, groupKey = null, title = title, year = year, episodeCount = episodeCount)
}
