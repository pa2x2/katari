package eu.kanade.tachiyomi.ui.video.player

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import mihon.entry.interactions.anime.animeProgressState
import org.junit.jupiter.api.Test

class VideoPlaybackSessionTest {

    @Test
    fun `restored baseline only records newly watched duration`() {
        val session = VideoPlaybackSession(entryId = 3L, chapterId = 7L, resourceKey = "/episode", now = { 1_000L })

        session.restore(30_000L)
        val snapshot = session.snapshot(positionMs = 45_000L, durationMs = 100_000L)

        snapshot.progressState.entryId shouldBe 3L
        snapshot.progressState.chapterId shouldBe 7L
        snapshot.progressState.resourceKey shouldBe "/episode"
        snapshot.progressState.locator.position shouldBe 45_000L
        snapshot.progressState.locator.extent shouldBe 100_000L
        snapshot.progressState.completed shouldBe false
        snapshot.progressState.locatorUpdatedAt shouldBe 1_000L
        snapshot.progressState.completionUpdatedAt shouldBe 0L
        snapshot.historyUpdate?.chapterId shouldBe 7L
        snapshot.historyUpdate?.sessionReadDuration shouldBe 15_000L
        snapshot.historyUpdate?.readAt?.time shouldBe 1_000L
    }

    @Test
    fun `history delta does not go negative after backwards seek baseline reset`() {
        val nowValues = ArrayDeque(listOf(2_000L, 3_000L))
        val session = VideoPlaybackSession(
            entryId = 3L,
            chapterId = 7L,
            resourceKey = "/episode",
            now = { nowValues.removeFirst() },
        )

        session.restore(40_000L)
        val snapshot = session.snapshot(positionMs = 10_000L, durationMs = 100_000L)

        snapshot.progressState.locator.position shouldBe 10_000L
        snapshot.historyUpdate.shouldBeNull()
    }

    @Test
    fun `completion threshold marks completed at ninety percent`() {
        val session = VideoPlaybackSession(entryId = 3L, chapterId = 7L, resourceKey = "/episode", now = { 4_000L })

        val snapshot = session.snapshot(positionMs = 90_000L, durationMs = 100_000L)

        snapshot.progressState.completed shouldBe true
        snapshot.completedNow shouldBe true
        snapshot.progressState.completionUpdatedAt shouldBe 4_000L
        snapshot.historyUpdate?.sessionReadDuration shouldBe 90_000L
    }

    @Test
    fun `backwards seek below threshold records a newer uncompletion event`() {
        val session = VideoPlaybackSession(entryId = 3L, chapterId = 7L, resourceKey = "/episode", now = { 8_000L })
        session.restore(
            animeProgressState(
                entryId = 3L,
                chapterId = 7L,
                resourceKey = "/episode",
                positionMs = 95_000L,
                durationMs = 100_000L,
                completed = true,
                locatorUpdatedAt = 7_000L,
                completionUpdatedAt = 7_000L,
            ),
        )

        val snapshot = session.snapshot(positionMs = 20_000L, durationMs = 100_000L)

        snapshot.progressState.completed shouldBe false
        snapshot.progressState.locator.position shouldBe 20_000L
        snapshot.progressState.locatorUpdatedAt shouldBe 8_000L
        snapshot.progressState.completionUpdatedAt shouldBe 8_000L
        snapshot.historyUpdate.shouldBeNull()
        snapshot.completedNow shouldBe false
    }
}
