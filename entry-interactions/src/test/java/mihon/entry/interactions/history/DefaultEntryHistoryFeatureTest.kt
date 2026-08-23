package mihon.entry.interactions.history

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.history.model.activity.HistoryCompletionCause
import tachiyomi.domain.history.model.activity.HistoryCompletionUpdate
import tachiyomi.domain.history.repository.HistoryRepository

class DefaultEntryHistoryFeatureTest {

    @Test
    fun `manual completion keeps child identity and cause in one repository batch`() = runTest {
        val repository = mockk<HistoryRepository>()
        val captured = slot<List<HistoryCompletionUpdate>>()
        coEvery { repository.recordCompletions(capture(captured)) } returns Unit
        val entry = Entry.create().copy(id = 7L, type = EntryType.BOOK)
        val children = listOf(
            EntryChapter.create().copy(id = 11L, entryId = entry.id),
            EntryChapter.create().copy(id = 12L, entryId = entry.id),
        )

        DefaultEntryHistoryFeature(repository).recordManualCompletions(entry, children)

        coVerify(exactly = 1) { repository.recordCompletions(any()) }
        captured.captured.map { it.chapterId } shouldContainExactly listOf(11L, 12L)
        captured.captured.map { it.entryId }.toSet() shouldBe setOf(entry.id)
        captured.captured.map { it.cause }.toSet() shouldBe setOf(HistoryCompletionCause.MANUAL)
        captured.captured.map { it.sessionId }.toSet() shouldBe setOf(null)
        captured.captured.map { it.eventId }.toSet().size shouldBe 2
    }
}
