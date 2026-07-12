package tachiyomi.data.video

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.data.entry.PlaybackStateMapper

class PlaybackStateMapperTest {

    @Test
    fun `mapState maps playback row fields`() {
        val state = PlaybackStateMapper.mapState(
            id = 1L,
            entryId = 2L,
            chapterId = 3L,
            positionMs = 4_000L,
            durationMs = 30_000L,
            completed = true,
            lastWatchedAt = 5_000L,
        )

        state.entryId shouldBe 2L
        state.chapterId shouldBe 3L
        state.positionMs shouldBe 4_000L
        state.durationMs shouldBe 30_000L
        state.completed shouldBe true
        state.lastWatchedAt shouldBe 5_000L
    }
}
