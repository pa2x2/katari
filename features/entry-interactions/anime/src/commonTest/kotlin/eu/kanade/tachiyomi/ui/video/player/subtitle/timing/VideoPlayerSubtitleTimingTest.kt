package eu.kanade.tachiyomi.ui.video.player.subtitle.timing

import eu.kanade.tachiyomi.source.entry.VideoRequest
import eu.kanade.tachiyomi.source.entry.VideoSubtitle
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerSubtitleSelection
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.SubtitleTiming
import tachiyomi.domain.entry.repository.SubtitleTimingRepository

@OptIn(ExperimentalCoroutinesApi::class)
class VideoPlayerSubtitleTimingTest {

    private val english = VideoPlayerSubtitleSelection.External(
        VideoSubtitle(VideoRequest("https://cdn.example.com/1/en.vtt"), label = "English", language = "en"),
    )
    private val spanish = VideoPlayerSubtitleSelection.External(
        VideoSubtitle(VideoRequest("https://cdn.example.com/1/es.vtt"), label = "Español", language = "es"),
    )

    @Test
    fun `steps held faster than the screen updates all count, and stepping back to none saves none`() = runTest {
        val repository = FakeTimings()
        val timing = timing(repository, episode = 1, subtitle = english)

        repeat(12) { timing.step(1) }
        timing.state.value.delayMs shouldBe 1_200L
        repeat(12) { timing.step(-1) }
        advanceUntilIdle()

        repository.stored shouldBe emptyMap()
        timing.state.value.delayMs shouldBe 0L

        timing.set(-1_500)
        advanceUntilIdle()
        repository.stored shouldBe mapOf(1L to SubtitleTiming(1, subtitleTimingKey(english)!!, -1_500))
    }

    @Test
    fun `turning subtitles off keeps the timing for when they're back, and picking another subtitle resets it`() =
        runTest {
            val repository = FakeTimings(SubtitleTiming(1, subtitleTimingKey(english)!!, 400))
            val subtitles = MutableStateFlow<VideoPlayerSubtitleSelection>(english)
            val timing = timing(repository, episodes = MutableStateFlow(1L), subtitles = subtitles)
            timing.state.value.delayMs shouldBe 400L

            subtitles.value = VideoPlayerSubtitleSelection.None
            timing.subtitlePicked(VideoPlayerSubtitleSelection.None)
            runCurrent()
            timing.state.value.delayMs shouldBe 0L
            subtitles.value = english
            timing.subtitlePicked(english)
            runCurrent()
            timing.state.value.delayMs shouldBe 400L

            timing.subtitlePicked(spanish)
            subtitles.value = spanish
            advanceUntilIdle()
            subtitles.value = english
            runCurrent()
            timing.state.value.delayMs shouldBe 0L
            repository.stored shouldBe emptyMap()
        }

    @Test
    fun `the notice tells of the timing an episode opened with, not of one set while watching`() = runTest {
        val repository = FakeTimings(SubtitleTiming(1, subtitleTimingKey(english)!!, 400))
        val episodes = MutableStateFlow(1L)
        val subtitles = MutableStateFlow<VideoPlayerSubtitleSelection>(VideoPlayerSubtitleSelection.None)
        val timing = timing(repository, episodes = episodes, subtitles = subtitles)
        timing.notice.value shouldBe null

        // The subtitle resolves after the episode loads
        subtitles.value = english
        runCurrent()
        timing.notice.value shouldBe 400L
        timing.dismissNotice()

        episodes.value = 2
        runCurrent()
        timing.set(700)
        subtitles.value = VideoPlayerSubtitleSelection.None
        runCurrent()
        subtitles.value = english
        runCurrent()
        timing.notice.value shouldBe null
        timing.state.value.delayMs shouldBe 700L
    }

    private fun TestScope.timing(
        repository: FakeTimings,
        episode: Long = 1,
        subtitle: VideoPlayerSubtitleSelection = english,
    ) = timing(repository, MutableStateFlow(episode), MutableStateFlow(subtitle))

    private fun TestScope.timing(
        repository: FakeTimings,
        episodes: MutableStateFlow<Long>,
        subtitles: MutableStateFlow<VideoPlayerSubtitleSelection>,
    ): VideoPlayerSubtitleTiming {
        val timing = VideoPlayerSubtitleTiming(
            scope = backgroundScope,
            timings = repository,
            episodes = episodes,
            subtitles = subtitles,
            persistenceScope = this,
            persistenceDispatcher = StandardTestDispatcher(testScheduler),
        )
        runCurrent()
        return timing
    }

    /** Keeps a timing per episode, as the table does, which holds no timing of 0. */
    private class FakeTimings(vararg initial: SubtitleTiming) : SubtitleTimingRepository {
        val stored = initial.associateBy { it.chapterId }.toMutableMap()

        override suspend fun get(chapterId: Long) = stored[chapterId]

        override suspend fun getByEntryId(entryId: Long) = stored.values.toList()

        override suspend fun set(timing: SubtitleTiming) {
            require(timing.delayMs != 0L)
            stored[timing.chapterId] = timing
        }

        override suspend fun delete(chapterId: Long) {
            stored.remove(chapterId)
        }
    }
}
