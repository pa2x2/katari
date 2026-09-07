package eu.kanade.tachiyomi.ui.reader.navigation

import eu.kanade.tachiyomi.ui.reader.navigation.MangaReaderJumpHistory.Position
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Test

class MangaReaderJumpHistoryTest {
    @Test
    fun `slider updates retain the page before the gesture and a later gesture replaces it`() {
        val history = MangaReaderJumpHistory()
        history.observe(chapterId = 10, pageIndex = 3)
        history.beginSeek(8)
        history.observe(chapterId = 10, pageIndex = 8)
        history.beginSeek(15)
        history.observe(chapterId = 10, pageIndex = 15)
        history.finishSeek()
        history.returnTarget shouldBe Position(10, 3)

        history.beginSeek(20)
        history.returnTarget shouldBe Position(10, 15)
    }

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

    @Test
    fun `failed chapter load neither creates a return point nor replaces an earlier successful origin`() = runTest {
        val history = MangaReaderJumpHistory()
        history.observe(chapterId = 10, pageIndex = 7)
        history.rememberSuccessfulJump { false } shouldBe false
        history.returnTarget shouldBe null

        history.rememberSuccessfulJump {
            history.observe(chapterId = 11, pageIndex = 0)
            true
        }
        history.rememberSuccessfulJump {
            yield()
            false
        } shouldBe false
        history.returnTarget shouldBe Position(10, 7)
    }

    @Test
    fun `touching the current slider position does not replace an existing return point`() {
        val history = MangaReaderJumpHistory()
        history.observe(chapterId = 10, pageIndex = 3)
        history.beginSeek(8)
        history.observe(chapterId = 10, pageIndex = 8)
        history.finishSeek()
        history.beginSeek(8)
        history.finishSeek()
        history.returnTarget shouldBe Position(10, 3)
    }
}
