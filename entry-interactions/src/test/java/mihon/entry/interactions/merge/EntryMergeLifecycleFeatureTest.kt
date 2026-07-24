package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.host.EntryMergeConsequenceStatusSnapshot
import mihon.entry.interactions.host.EntryMergeHostTransition
import mihon.entry.interactions.host.EntryMergeMembershipSnapshot
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry

class EntryMergeLifecycleFeatureTest {
    @Test
    fun `library removal groups selected members and dissolves a depleted membership`() = runTest {
        val entries = listOf(entry(1), entry(2), entry(3))
        val host = RecordingEntryMergeHost(entries, listOf(EntryMergeMembershipSnapshot(7, 1, listOf(1, 2, 3))))

        EntryMergeLibraryLifecycleCoordinator(host).entriesRemovedFromLibrary(listOf(entries[0], entries[2])) shouldBe
            EntryMergeLibraryRemovalResult(changedGroupCount = 1, unresolvedGroupCount = 0)
        host.transitions.single().shouldBeInstanceOf<EntryMergeHostTransition.ChangeExistingGroup>().run {
            replacementTargetEntryId shouldBe null
            replacementOrderedEntryIds shouldBe emptyList()
        }
    }

    @Test
    fun `metadata refresh receives ordered concrete owners without using the editor`() = runTest {
        val entries = listOf(entry(1), entry(2), entry(3))
        val host = RecordingEntryMergeHost(entries, listOf(EntryMergeMembershipSnapshot(7, 2, listOf(2, 1, 3))))

        EntryMergeMetadataRefreshCoordinator(host).resolveOwners(entries.first()).run {
            visibleEntryId shouldBe 2
            orderedOwners.map(Entry::id) shouldContainExactly listOf(2, 1, 3)
        }
    }

    @Test
    fun `status surface aggregates diagnostics and retries through the owned delivery gate`() = runTest {
        val host = RecordingEntryMergeHost(
            entries = emptyList(),
            initialStatus = EntryMergeConsequenceStatusSnapshot(3, 2, "disk unavailable"),
        )
        val delivery = EntryMergeConsequenceDelivery(host, mockk(relaxed = true))
        val feature = EntryMergeConsequenceStatusCoordinator(host, delivery)

        feature.observeStatus().first() shouldBe EntryMergeConsequenceStatus(3, 2, "disk unavailable")
        feature.retryPending() shouldBe EntryMergeConsequenceStatus(3, 2, "disk unavailable")
        host.madeRetryable shouldBe true
    }
}

private fun entry(id: Long): Entry {
    return Entry.create().copy(
        id = id,
        profileId = 7,
        source = 10,
        url = "/$id",
        title = "$id",
        favorite = true,
        type = EntryType.BOOK,
    )
}
