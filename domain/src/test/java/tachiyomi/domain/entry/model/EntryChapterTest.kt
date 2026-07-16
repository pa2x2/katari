package tachiyomi.domain.entry.model

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Test

class EntryChapterTest {

    @Test
    fun `copyFrom copies source memo`() {
        val memo = buildJsonObject { put("key", JsonPrimitive("value")) }

        val copied = EntryChapter.create().copyFrom(EntryChapter.create().copy(memo = memo))

        copied.memo shouldBe memo
    }

    @Test
    fun `progress resource key falls back to persisted chapter id for blank urls`() {
        EntryChapter.create().copy(id = 42L, url = "  ").progressResourceKey shouldBe "legacy-chapter:42"
    }

    @Test
    fun `progress resource key preserves nonblank chapter url`() {
        EntryChapter.create().copy(id = 42L, url = "/chapter/42").progressResourceKey shouldBe "/chapter/42"
    }
}
