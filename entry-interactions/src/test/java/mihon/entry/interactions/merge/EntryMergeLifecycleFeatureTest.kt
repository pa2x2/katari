package mihon.entry.interactions.merge

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.merge.host.EntryMergeHostTransition
import mihon.entry.interactions.merge.host.EntryMergeMembershipSnapshot
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry

class EntryMergeLifecycleFeatureTest {
    @Test
    fun `library removal groups selected members and dissolves a depleted membership`() = runTest {
        val entries = listOf(entry(1), entry(2), entry(3))
        val host = RecordingEntryMergeHost(
            entries,
            listOf(EntryMergeMembershipSnapshot(7, 1, listOf(1, 2, 3))),
        )

        EntryMergeLibraryLifecycleCoordinator(host).entriesRemovedFromLibrary(listOf(entries[0], entries[2])) shouldBe
            EntryMergeLibraryRemovalResult(changedGroupCount = 1, unresolvedGroupCount = 0)
        host.transitions.single().shouldBeInstanceOf<EntryMergeHostTransition.ChangeExistingGroup>().run {
            replacementTargetEntryId shouldBe null
            replacementOrderedEntryIds shouldBe emptyList()
        }
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
