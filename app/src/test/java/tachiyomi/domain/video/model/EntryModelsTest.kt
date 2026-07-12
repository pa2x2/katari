package tachiyomi.domain.entry.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class EntryModelsTest {

    @Test
    fun `entry create has safe defaults`() {
        val entry = Entry.create()

        entry.id shouldBe -1L
        entry.source shouldBe -1L
        entry.favorite shouldBe false
        entry.url shouldBe ""
        entry.title shouldBe ""
        entry.displayTitle shouldBe ""
    }

    @Test
    fun `entry chapter create has safe defaults`() {
        val episode = EntryChapter.create()

        episode.id shouldBe -1L
        episode.entryId shouldBe -1L
        episode.read shouldBe false
        episode.bookmark shouldBe false
        episode.url shouldBe ""
        episode.chapterNumber shouldBe -1.0
    }
}
