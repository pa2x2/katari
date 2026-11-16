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

    @Test
    fun `reading saved after a position dated ahead of this device's clock arrived replaces it, dated after it`() {
        // Synced from a device whose clock is ahead: read at 5000 there, while it is 4000 here
        val held = state(
            locator = EntryProgressLocator(kind = "page", position = 30),
            completed = false,
            locatorUpdatedAt = 5_000,
            completionUpdatedAt = 5_000,
        )
        val reading = state(
            locator = EntryProgressLocator(kind = "page", position = 45),
            completed = true,
            locatorUpdatedAt = 4_000,
            completionUpdatedAt = 0,
        )

        val edited = held.withEdit(reading)

        edited.locator.position shouldBe 45
        edited.locatorUpdatedAt shouldBe 5_001
        // The reading didn't set completion, so what is held stays
        edited.completed shouldBe false
        edited.completionUpdatedAt shouldBe 5_000
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
