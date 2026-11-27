package eu.kanade.tachiyomi.data.backup.conversion.lnreader.settings

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry

class LnReaderChapterListTest {

    @Test
    fun `a novel's filters keep the chapters LNReader showed, oldest first by position`() {
        // Katari's source order counts from the newest chapter, LNReader's positions from the oldest
        val list = LnReaderChapterList(
            LnReaderNovelSettings(filter = listOf("read", "not-downloaded"), showChapterTitles = false),
            LnReaderChapterOrder.of("ORDER BY position ASC")!!,
        )

        list.chapterFlags shouldBe (
            Entry.CHAPTER_SORTING_SOURCE or Entry.CHAPTER_SORT_ASC or Entry.CHAPTER_SHOW_READ or
                Entry.CHAPTER_SHOW_NOT_DOWNLOADED or Entry.CHAPTER_DISPLAY_NUMBER
            )
    }

    @Test
    fun `a read time order Katari can't sort by gives way to the app's default and is reported`() {
        val list = LnReaderChapterList(LnReaderNovelSettings(sort = "readTimeDesc"), LnReaderChapterOrder.POSITION_DESC)

        list.unsortable shouldBe true
        list.chapterFlags shouldBe (Entry.CHAPTER_SORTING_SOURCE or Entry.CHAPTER_SORT_DESC)
    }
}
