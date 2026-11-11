package app.katari.entry.interactions.anime.introtimes.theintrodb

import app.katari.entry.interactions.anime.introtimes.IntroTimesScope
import app.katari.entry.interactions.anime.introtimes.seasonNumber
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.TheIntroDbPlacement
import java.time.LocalDate

class TheIntroDbPlacementGuessTest {

    @Test
    fun `a second part numbered from 1 is placed where it aired, though its source dates it a day late`() {
        // TVmaze's listing for Attack on Titan around the Final Season's two parts, 2026-11-08.
        val aired = listOf(
            AiredEpisode(season = 4, number = 16, name = "Heaven and Earth", aired = LocalDate.of(2021, 3, 28)),
            AiredEpisode(season = 4, number = 17, name = "Judgement", aired = LocalDate.of(2022, 1, 9)),
            AiredEpisode(season = 4, number = 18, name = "Sneak Attack", aired = LocalDate.of(2022, 1, 16)),
        )

        guessPlacement(scope(firstAired = LocalDate.of(2022, 1, 10), seasonName = null), aired) shouldBe
            TheIntroDbPlacement(season = 4, firstEpisode = 17)
    }

    @Test
    fun `without dates the season comes from the group's name, in English or Russian`() {
        guessPlacement(scope(firstAired = null, seasonName = "2nd Season"), emptyList()) shouldBe
            TheIntroDbPlacement(season = 2, firstEpisode = 1)
        seasonNumber("Сезон 3") shouldBe 3
        seasonNumber("4 сезон") shouldBe 4
        seasonNumber("Specials") shouldBe null
    }

    private fun scope(firstAired: LocalDate?, seasonName: String?) = IntroTimesScope(
        entryId = 1,
        groupKey = null,
        title = "Attack on Titan: The Final Season Part 2",
        seasonName = seasonName,
        year = firstAired?.year,
        episodeCount = 12,
        firstNumber = 1,
        firstAired = firstAired,
    )
}
