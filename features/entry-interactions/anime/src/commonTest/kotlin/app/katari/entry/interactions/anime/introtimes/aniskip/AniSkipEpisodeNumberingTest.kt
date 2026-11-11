package app.katari.entry.interactions.anime.introtimes.aniskip

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AniSkipEpisodeNumberingTest {

    // AniList's seasons of Attack on Titan up to the Final Season's first part, 2026-11-11.
    private val seasons = listOf(
        season(malId = 16498, episodes = 25),
        season(malId = 25777, episodes = 12),
        season(malId = 35760, episodes = 12),
        season(malId = 38524, episodes = 10),
        season(malId = 40028, episodes = null),
    )

    @Test
    fun `an episode numbered through the show from its first season goes by AniSkip's own numbering`() {
        // AniSkip's answer for /v2/relation-rules/16498, 2026-11-11, cut to its first two rules.
        val numberings = aniSkipNumberings(
            """
            {"statusCode":200,"message":"Successfully found rules for animeId '16498'","found":true,"rules":[
            {"from":{"start":26,"end":37},"to":{"malId":25777,"start":1,"end":12}},
            {"from":{"start":38,"end":49},"to":{"malId":35760,"start":1,"end":12}}]}
            """.trimIndent(),
        )

        episodeInSeasons(seasons, index = 0, episode = 30, numberings) shouldBe AniSkipEpisode(25777, 5)
        // Past what AniSkip numbers, the seasons' lengths are counted.
        episodeInSeasons(seasons, index = 0, episode = 50, numberings) shouldBe AniSkipEpisode(38524, 1)
    }

    @Test
    fun `an episode outside a later season is counted on or back through the seasons' lengths`() {
        episodeInSeasons(seasons, index = 1, episode = 13, emptyList()) shouldBe AniSkipEpisode(35760, 1)
        episodeInSeasons(seasons, index = 2, episode = 0, emptyList()) shouldBe AniSkipEpisode(25777, 12)
        // A season still airing takes any number; before the first season there is nothing.
        episodeInSeasons(seasons, index = 3, episode = 40, emptyList()) shouldBe AniSkipEpisode(40028, 30)
        episodeInSeasons(seasons, index = 1, episode = -30, emptyList()) shouldBe null
    }

    private fun season(malId: Long, episodes: Int?) = AniListSeries(
        anilistId = malId + 100_000,
        malId = malId,
        title = "Shingeki no Kyojin",
        titles = emptyList(),
        year = null,
        startOrder = 0,
        format = "TV",
        episodes = episodes,
        airedEpisodes = null,
        coverUrl = null,
        episodeNames = emptyMap(),
        prequels = emptyList(),
        sequels = emptyList(),
    )
}
