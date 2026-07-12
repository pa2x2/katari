package eu.kanade.tachiyomi.ui.video.player

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class VideoPlaybackSessionTest {

    @Test
    fun `restored baseline only records newly watched duration`() {
        val session = VideoPlaybackSession(entryId = 3L, chapterId = 7L, now = { 1_000L })

        session.restore(30_000L)
        val snapshot = session.snapshot(positionMs = 45_000L, durationMs = 100_000L)

        snapshot.playbackState.entryId shouldBe 3L
        snapshot.playbackState.chapterId shouldBe 7L
        snapshot.playbackState.positionMs shouldBe 45_000L
        snapshot.playbackState.durationMs shouldBe 100_000L
        snapshot.playbackState.completed shouldBe false
        snapshot.playbackState.lastWatchedAt shouldBe 1_000L
        snapshot.historyUpdate?.chapterId shouldBe 7L
        snapshot.historyUpdate?.sessionReadDuration shouldBe 15_000L
        snapshot.historyUpdate?.readAt?.time shouldBe 1_000L
    }

    @Test
    fun `history delta does not go negative after backwards seek baseline reset`() {
        val nowValues = ArrayDeque(listOf(2_000L, 3_000L))
        val session = VideoPlaybackSession(entryId = 3L, chapterId = 7L, now = { nowValues.removeFirst() })

        session.restore(40_000L)
        val snapshot = session.snapshot(positionMs = 10_000L, durationMs = 100_000L)

        snapshot.playbackState.positionMs shouldBe 10_000L
        snapshot.historyUpdate.shouldBeNull()
    }

    @Test
    fun `completion threshold marks completed at ninety percent`() {
        val session = VideoPlaybackSession(entryId = 3L, chapterId = 7L, now = { 4_000L })

        val snapshot = session.snapshot(positionMs = 90_000L, durationMs = 100_000L)

        snapshot.playbackState.completed shouldBe true
        snapshot.historyUpdate?.sessionReadDuration shouldBe 90_000L
    }
}
