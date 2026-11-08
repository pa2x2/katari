package mihon.entry.interactions.anime.stream

import eu.kanade.tachiyomi.source.entry.VideoRequest
import eu.kanade.tachiyomi.source.entry.VideoStream
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.io.IOException

class LateStreamResolutionTest {

    @Test
    fun `a stream the user picked that can't be resolved fails instead of playing another one`() = runTest {
        val broken = VideoStream(request = null, key = "1080p")
        val working = VideoStream(request = null, key = "720p")
        val resolve: suspend (VideoStream) -> VideoStream = { stream ->
            if (stream == broken) throw IOException("expired")
            stream.copy(request = VideoRequest("https://cdn.example.com/${stream.key}.mp4"))
        }

        firstPlayableStream(listOf(broken, working), explicitPick = true, resolve).isFailure shouldBe true
        firstPlayableStream(listOf(broken, working), explicitPick = false, resolve).getOrNull()?.key shouldBe "720p"
    }
}
