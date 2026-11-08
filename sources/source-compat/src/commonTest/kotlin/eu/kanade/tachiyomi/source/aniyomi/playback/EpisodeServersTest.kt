package eu.kanade.tachiyomi.source.aniyomi.playback

import eu.kanade.tachiyomi.animesource.model.Hoster
import eu.kanade.tachiyomi.animesource.model.Video
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class EpisodeServersTest {

    @Test
    fun `an episode opens on the server with the preferred video and never fetches lazy servers or listed videos`() =
        runTest {
            val fetched = mutableListOf<String>()
            val videosByServer = mapOf(
                "Fast" to listOf(Video(videoUrl = "https://a.example/720.mp4", videoTitle = "720p")),
                "Preferred" to listOf(
                    Video(videoUrl = "https://b.example/1080.mp4", videoTitle = "1080p", preferred = true),
                ),
            )
            val hosters = listOf(
                Hoster(hosterUrl = "https://a.example/e1", hosterName = "Fast"),
                Hoster(hosterUrl = "https://b.example/e1", hosterName = "Preferred"),
                Hoster(hosterUrl = "https://c.example/e1", hosterName = "Rezka", lazy = true),
            )
            val servers = EpisodeServers(hosters, scope = this, sortVideos = { it }) { hoster ->
                fetched += hoster.hosterName
                videosByServer.getValue(hoster.hosterName)
            }
            // Extensions-lib 14 videos wrapped as Aniyomi's player expects, with an empty hoster URL
            val lib14 = EpisodeServers(
                listOf(Hoster(hosterName = Hoster.NO_HOSTER_LIST, videoList = listOf(Video("/e1", "480p", "null")))),
                scope = this,
                sortVideos = { it },
            ) { error("A hoster that came with its videos was fetched") }

            val opened = servers.open(requestedKey = null, deadlineMs = 10_000)
            val lib14Opened = lib14.open(requestedKey = null, deadlineMs = 10_000)

            opened?.server?.key shouldBe "Preferred"
            fetched.sorted() shouldBe listOf("Fast", "Preferred")
            lib14.showsServers shouldBe false
            lib14Opened?.videos?.map { it.videoTitle } shouldBe listOf("480p")
        }
}
