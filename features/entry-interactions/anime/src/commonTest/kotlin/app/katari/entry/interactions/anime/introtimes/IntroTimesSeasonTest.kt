package app.katari.entry.interactions.anime.introtimes

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class IntroTimesSeasonTest {

    @Test
    fun `a title's season is read from its end, and a number that names the show or a part isn't taken for one`() {
        titleSeason("Bungou Stray Dogs 5th Season") shouldBe TitleSeason("Bungou Stray Dogs", 5)
        titleSeason("Overlord II") shouldBe TitleSeason("Overlord", 2)
        titleSeason("Магическая битва 2") shouldBe TitleSeason("Магическая битва", 2)
        titleSeason("Dr. Stone: Season 3") shouldBe TitleSeason("Dr. Stone", 3)

        titleSeason("Kaiju No. 8") shouldBe null
        titleSeason("Mob Psycho 100") shouldBe null
        titleSeason("Attack on Titan Season 3 Part 2") shouldBe null
    }
}
