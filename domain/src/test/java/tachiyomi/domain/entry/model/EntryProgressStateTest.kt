package tachiyomi.domain.entry.model

import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EntryProgressStateTest {
    @Test
    fun `locator rejects invalid common values`() {
        assertThrows<IllegalArgumentException> { EntryProgressLocator(kind = "") }
        assertThrows<IllegalArgumentException> { EntryProgressLocator(kind = "page", position = -1) }
        assertThrows<IllegalArgumentException> { EntryProgressLocator(kind = "page", extent = 0) }
        assertThrows<IllegalArgumentException> { EntryProgressLocator(kind = "page", progression = Double.NaN) }
        assertThrows<IllegalArgumentException> { EntryProgressLocator(kind = "page", totalProgression = 1.1) }
    }

    @Test
    fun `merge resolves locator and completion with independent clocks`() {
        val current = state(
            locator = EntryProgressLocator(kind = "time", position = 100),
            completed = true,
            locatorUpdatedAt = 10,
            completionUpdatedAt = 30,
        )
        val incoming = state(
            locator = EntryProgressLocator(kind = "time", position = 200),
            completed = false,
            locatorUpdatedAt = 20,
            completionUpdatedAt = 25,
        )

        val merged = current.mergeWith(incoming)

        merged.locator.position shouldBe 200
        merged.locatorUpdatedAt shouldBe 20
        merged.completed shouldBe true
        merged.completionUpdatedAt shouldBe 30
    }

    @Test
    fun `merge keeps local fields on exact clock ties`() {
        val current = state(
            locator = EntryProgressLocator(kind = "page", position = 4),
            completed = false,
            locatorUpdatedAt = 10,
            completionUpdatedAt = 10,
        )
        val incoming = state(
            locator = EntryProgressLocator(kind = "page", position = 8),
            completed = true,
            locatorUpdatedAt = 10,
            completionUpdatedAt = 10,
        )

        current.mergeWith(incoming) shouldBe current
    }

    @Test
    fun `locator serialization preserves unknown extensions`() {
        val locator = EntryProgressLocator(
            kind = "reader.example",
            extensions = buildJsonObject {
                put("reader.example.precise", JsonPrimitive("opaque"))
            },
        )

        val restored = Json.decodeFromString<EntryProgressLocator>(Json.encodeToString(locator))

        restored shouldBe locator
    }

    @Test
    fun `empty locator represents a persistent reset tombstone`() {
        val reset = state(
            locator = EntryProgressLocator(kind = "page"),
            completed = false,
            locatorUpdatedAt = 40,
            completionUpdatedAt = 40,
        )

        reset.locator.isEmpty shouldBe true
    }

    private fun state(
        locator: EntryProgressLocator,
        completed: Boolean,
        locatorUpdatedAt: Long,
        completionUpdatedAt: Long,
    ): EntryProgressState {
        return EntryProgressState(
            entryId = 1,
            chapterId = 2,
            resourceKey = "/chapter",
            locator = locator,
            completed = completed,
            locatorUpdatedAt = locatorUpdatedAt,
            completionUpdatedAt = completionUpdatedAt,
        )
    }
}
