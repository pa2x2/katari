package eu.kanade.tachiyomi.ui.reader.navigation

import eu.kanade.tachiyomi.ui.reader.navigation.MangaReaderJumpHistory.Position
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Test

class MangaReaderJumpHistoryTest {
    @Test
    fun `successful chapter load retains its origin even when loading observes the destination`() = runTest {
        val history = MangaReaderJumpHistory()
        history.observe(chapterId = 10, pageIndex = 7)
        history.rememberSuccessfulJump {
            yield()
            history.observe(chapterId = 11, pageIndex = 0)
            true
        } shouldBe true
        history.observe(chapterId = 12, pageIndex = 4)
        history.returnTarget shouldBe Position(10, 7)

        history.dismiss()
        history.observe(chapterId = 12, pageIndex = 5)
        history.returnTarget shouldBe null

        history.rememberSuccessfulJump {
            history.observe(chapterId = 13, pageIndex = 0)
            true
        }
        history.returnTarget shouldBe Position(12, 5)
    }
}
