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
}
