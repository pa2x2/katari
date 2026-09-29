package tachiyomi.domain.entry.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class EntryProgressStateTest {
    @Test
    fun `merge resolves locator and completion with independent clocks and keeps local fields on ties`() {
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

        val tiedCurrent = state(
            locator = EntryProgressLocator(kind = "page", position = 4),
            completed = false,
            locatorUpdatedAt = 10,
            completionUpdatedAt = 10,
        )
        val tiedIncoming = state(
            locator = EntryProgressLocator(kind = "page", position = 8),
            completed = true,
            locatorUpdatedAt = 10,
            completionUpdatedAt = 10,
        )

        tiedCurrent.mergeWith(tiedIncoming) shouldBe tiedCurrent
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
