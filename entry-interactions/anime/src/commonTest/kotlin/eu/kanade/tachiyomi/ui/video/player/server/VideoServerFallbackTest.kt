package eu.kanade.tachiyomi.ui.video.player.server

import eu.kanade.tachiyomi.source.entry.VideoPlaybackOption
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class VideoServerFallbackTest {

    @Test
    fun `failing servers don't take turns, fallback stops after three, and a picked server is never left`() {
        val servers = listOf("a", "b", "c", "d").map { VideoPlaybackOption(key = it, label = it.uppercase()) }
        val (a, b, c, d) = servers

        val first = ServerFallback().next(servers, failed = b)!!
        first.next shouldBe c
        val second = first.fallback.next(servers, failed = c)!!
        second.next shouldBe d
        second.fallback.next(servers, failed = d) shouldBe null

        val twoServers = servers.take(2)
        ServerFallback().next(twoServers, failed = a)!!.fallback.next(twoServers, failed = b) shouldBe null

        ServerFallback(explicitPick = true).next(servers, failed = a) shouldBe null
    }
}
