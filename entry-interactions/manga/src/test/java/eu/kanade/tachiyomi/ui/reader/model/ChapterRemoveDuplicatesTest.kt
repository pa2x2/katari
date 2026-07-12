package eu.kanade.tachiyomi.ui.reader.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter

class ChapterRemoveDuplicatesTest {

    @Test
    fun `keeps same-number chapters from different merged members`() {
        val currentChapter = chapter(id = 101, mangaId = 1, chapterNumber = 1.0, scanlator = "A")
        val mergedMemberChapter = chapter(id = 201, mangaId = 2, chapterNumber = 1.0, scanlator = "B")

        listOf(currentChapter, mergedMemberChapter)
            .removeDuplicates(currentChapter)
            .map(Chapter::id) shouldBe listOf(101L, 201L)
    }

    @Test
    fun `removes same-number duplicates within the same manga`() {
        val currentChapter = chapter(id = 101, mangaId = 1, chapterNumber = 1.0, scanlator = "A")
        val duplicateChapter = chapter(id = 102, mangaId = 1, chapterNumber = 1.0, scanlator = "B")
        val nextChapter = chapter(id = 103, mangaId = 1, chapterNumber = 2.0, scanlator = null)

        listOf(currentChapter, duplicateChapter, nextChapter)
            .removeDuplicates(currentChapter)
            .map(Chapter::id) shouldBe listOf(101L, 103L)
    }

    private fun chapter(
        id: Long,
        mangaId: Long,
        chapterNumber: Double,
        scanlator: String?,
    ): Chapter {
        return Chapter.create().copy(
            id = id,
            mangaId = mangaId,
            chapterNumber = chapterNumber,
            scanlator = scanlator,
            name = "Chapter $id",
            url = "/chapter/$id",
        )
    }
}
