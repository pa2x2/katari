package eu.kanade.tachiyomi.ui.reader.navigation

import eu.kanade.tachiyomi.ui.reader.navigation.MangaReaderJumpHistory.Position
import io.kotest.matchers.shouldBe
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
    fun `reading across chapters preserves the explicit jump origin until dismissed`() {
        val history = MangaReaderJumpHistory()
        history.observe(chapterId = 10, pageIndex = 7)
        history.rememberOrigin()
        history.observe(chapterId = 11, pageIndex = 0)
        history.observe(chapterId = 12, pageIndex = 4)
        history.returnTarget shouldBe Position(10, 7)

        history.dismiss()
        history.observe(chapterId = 12, pageIndex = 5)
        history.returnTarget shouldBe null

        history.rememberOrigin()
        history.returnTarget shouldBe Position(12, 5)
    }

    @Test
    fun `touching the current slider position does not replace an existing return point`() {
        val history = MangaReaderJumpHistory()
        history.observe(chapterId = 10, pageIndex = 3)
        history.rememberOrigin()
        history.observe(chapterId = 10, pageIndex = 8)
        history.beginSeek(8)
        history.finishSeek()
        history.returnTarget shouldBe Position(10, 3)
    }
}
