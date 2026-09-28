package eu.kanade.tachiyomi.ui.reader.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter

class ChapterRemoveDuplicatesTest {

    @Test
    fun `removes same-number duplicates within a manga but keeps them across merged members`() {
        val currentChapter = chapter(id = 101, mangaId = 1, chapterNumber = 1.0, scanlator = "A")
        val duplicateChapter = chapter(id = 102, mangaId = 1, chapterNumber = 1.0, scanlator = "B")
        val nextChapter = chapter(id = 103, mangaId = 1, chapterNumber = 2.0, scanlator = null)
        val mergedMemberChapter = chapter(id = 201, mangaId = 2, chapterNumber = 1.0, scanlator = "B")

        listOf(currentChapter, duplicateChapter, nextChapter, mergedMemberChapter)
            .removeDuplicates(currentChapter)
            .map(Chapter::id) shouldBe listOf(101L, 103L, 201L)
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
