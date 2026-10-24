package mihon.entry.interactions.anime.stream

import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.VideoRequest
import eu.kanade.tachiyomi.source.entry.VideoStream
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ResolvedStreamCacheTest {

    @Test
    fun `a link resolved for one episode or dub isn't played for another with the same stream key`() = runTest {
        val cache = ResolvedStreamCache()
        val stream = VideoStream(request = null, key = "1080p")
        val japanese = PlaybackSelection(dubKey = "ja", streamKey = "1080p")
        var resolutions = 0
        val resolve: suspend (VideoStream) -> VideoStream = {
            resolutions++
            it.copy(request = VideoRequest("https://cdn.example.com/$resolutions.m3u8"))
        }

        val first = cache.getOrResolve(chapterId = 1, japanese, stream, resolve)
        cache.getOrResolve(chapterId = 1, japanese.copy(streamKey = null), stream, resolve) shouldBe first
        cache.getOrResolve(chapterId = 2, japanese, stream, resolve) shouldBe
            stream.copy(request = VideoRequest("https://cdn.example.com/2.m3u8"))
        cache.getOrResolve(chapterId = 1, japanese.copy(dubKey = "en"), stream, resolve) shouldBe
            stream.copy(request = VideoRequest("https://cdn.example.com/3.m3u8"))
        resolutions shouldBe 3
    }
}
