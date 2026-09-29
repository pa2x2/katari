package eu.kanade.tachiyomi.ui.video.player

import io.kotest.matchers.shouldBe
import mihon.entry.interactions.anime.state.animeProgressState
import org.junit.jupiter.api.Test

class VideoPlaybackSessionTest {

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
        snapshot.progressState.locatorUpdatedAt shouldBe 8_000L
        snapshot.progressState.completionUpdatedAt shouldBe 8_000L
        snapshot.completedNow shouldBe false
    }
}
