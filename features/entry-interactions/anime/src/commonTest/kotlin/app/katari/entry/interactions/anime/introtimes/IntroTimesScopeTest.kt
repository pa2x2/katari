package app.katari.entry.interactions.anime.introtimes

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

class IntroTimesScopeTest {

    @Test
    fun `a second season numbered on from the first asks AniSkip from 1, and a half episode isn't asked`() {
        val entry = Entry.create().copy(id = 7, title = "Sousou no Frieren")
        val firstSeason = (1..12).map { episode(it.toDouble(), "s1") }
        val secondSeason = (13..24).map { episode(it.toDouble(), "s2") } + episode(18.5, "s2")
        val episodes = firstSeason + secondSeason

        val thirteenth = introTimesEpisode(entry, secondSeason.first(), episodes, mapOf("s2" to "Season 2"))
        thirteenth.number shouldBe 1
        thirteenth.scope.groupKey shouldBe "s2"
        thirteenth.scope.seasonName shouldBe "Season 2"
        thirteenth.scope.episodeCount shouldBe 12
        thirteenth.scope.firstNumber shouldBe 13

        introTimesEpisode(entry, secondSeason.last(), episodes, emptyMap()).number shouldBe null
    }

    @Test
    fun `seasons a source lists together are matched one by one, by the season their episodes' names start with`() {
        // HexaWatch's Attack on Titan, 2026-11-11: every season in one list, each numbered from 1, in no groups.
        val entry = Entry.create().copy(id = 7, title = "Attack on Titan")
        val episodes = listOf(
            named(1, "S1 E21 - Crushing Blow: The 57th Exterior Scouting Mission (5)", 21.0),
            named(2, "S3 E20 - That Day", 20.0),
            named(3, "S3 E21 - The Attack Titan", 21.0),
            named(4, "S3 E22 - The Other Side of the Wall", 22.0),
            named(5, "S4 E21 - From You, 2,000 Years Ago", 21.0),
        )

        val playing = introTimesEpisode(entry, episodes[2], episodes, emptyMap())
        playing.scope.listedSeason shouldBe 3
        playing.scope.groupKey shouldBe "S3"
        playing.scope.episodeCount shouldBe 3
        playing.scope.firstNumber shouldBe 20
        playing.number shouldBe 2
    }

    @Test
    fun `the day a source put a finished season up isn't taken for when it aired`() {
        // AnimePahe's Attack on Titan Season 3 Part 2, which aired in 2019: all ten dated 2020-12-13, seconds apart.
        val entry = Entry.create().copy(id = 7, title = "Attack on Titan Season 3 Part 2")
        val putUp = 1_607_831_731_000L
        val episodes = (1..10).map { number ->
            EntryChapter.create().copy(
                id = number.toLong(),
                entryId = 7,
                chapterNumber = number.toDouble(),
                dateUpload = putUp + number * 25_000L,
            )
        }

        val scope = introTimesEpisode(entry, episodes[5], episodes, emptyMap()).scope
        scope.year shouldBe null
        scope.firstAired shouldBe null
    }

    private fun episode(number: Double, group: String) =
        EntryChapter.create().copy(id = (number * 10).toLong(), entryId = 7, chapterNumber = number, groupKey = group)

    private fun named(id: Long, name: String, number: Double) =
        EntryChapter.create().copy(id = id, entryId = 7, name = name, chapterNumber = number)
}
