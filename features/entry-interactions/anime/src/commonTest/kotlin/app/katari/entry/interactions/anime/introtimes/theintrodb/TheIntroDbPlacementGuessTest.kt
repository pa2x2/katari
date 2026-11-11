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
        val scope = scope(
            title = "Attack on Titan: The Final Season Part 2",
            episodes = 2,
            firstAired = LocalDate.of(2022, 1, 10),
            lastAired = LocalDate.of(2022, 1, 17),
        )

        guessPlacement(scope, aired) shouldBe TheIntroDbPlacement(season = 4, firstEpisode = 17)
    }

    @Test
    fun `a part put up long after it aired starts after its season's break, not where an episode aired that day`() {
        // TVmaze's listing for Attack on Titan, 2026-11-11. AnimePahe put Season 3 Part 2 up on 2020-12-13, the day
        // the fourth season's second episode aired.
        val aired = listOf(
            AiredEpisode(3, 11, "Bystander", LocalDate.of(2018, 10, 7)),
            AiredEpisode(3, 12, "Night of the Battle to Retake the Wall", LocalDate.of(2018, 10, 14)),
            AiredEpisode(3, 13, "The Town Where Everything Began", LocalDate.of(2019, 4, 28)),
            AiredEpisode(3, 14, "Thunder Spears", LocalDate.of(2019, 5, 5)),
            AiredEpisode(4, 1, "Other Side of the Sea", LocalDate.of(2020, 12, 6)),
            AiredEpisode(4, 2, "Midnight Train", LocalDate.of(2020, 12, 13)),
            AiredEpisode(4, 3, "The Door of Hope", LocalDate.of(2020, 12, 20)),
        )
        val putUp = scope(
            title = "Attack on Titan Season 3 Part 2",
            episodes = 2,
            firstAired = LocalDate.of(2020, 12, 13),
            lastAired = LocalDate.of(2020, 12, 14),
        )

        guessPlacement(putUp, aired) shouldBe TheIntroDbPlacement(season = 3, firstEpisode = 13)
        // A third part the season never had can't be placed.
        guessPlacement(putUp.copy(title = "Attack on Titan Season 3 Part 3"), aired) shouldBe null
    }

    @Test
    fun `without dates the season comes from the source's listing, in English or Russian`() {
        val listed = scope(title = "Attack on Titan", episodes = 12, firstAired = null, lastAired = null)
            .copy(listedSeason = seasonNumber("2nd Season"))

        guessPlacement(listed, emptyList()) shouldBe TheIntroDbPlacement(season = 2, firstEpisode = 1)
        seasonNumber("Сезон 3") shouldBe 3
        seasonNumber("4 сезон") shouldBe 4
        seasonNumber("Specials") shouldBe null
    }

    private fun scope(title: String, episodes: Int, firstAired: LocalDate?, lastAired: LocalDate?) = IntroTimesScope(
        entryId = 1,
        groupKey = null,
        title = title,
        seasonName = null,
        listedSeason = null,
        year = firstAired?.year,
        episodeCount = episodes,
        firstNumber = 1,
        lastNumber = episodes,
        firstAired = firstAired,
        lastAired = lastAired,
    )
}
