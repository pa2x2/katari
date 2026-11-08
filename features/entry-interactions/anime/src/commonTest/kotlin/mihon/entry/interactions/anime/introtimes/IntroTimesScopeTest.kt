package mihon.entry.interactions.anime.introtimes

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

    private fun episode(number: Double, group: String) =
        EntryChapter.create().copy(id = (number * 10).toLong(), entryId = 7, chapterNumber = number, groupKey = group)
}
