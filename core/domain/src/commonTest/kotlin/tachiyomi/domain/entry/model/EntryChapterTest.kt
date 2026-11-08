package tachiyomi.domain.entry.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class EntryChapterTest {

    @Test
    fun `progress resource key falls back to persisted chapter id for blank urls`() {
        EntryChapter.create().copy(id = 42L, url = "  ").progressResourceKey shouldBe "legacy-chapter:42"
    }
}
