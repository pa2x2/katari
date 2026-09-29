package mihon.entry.interactions.merge

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.merge.host.EntryMergeMembershipSnapshot
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry

class EntryMergeProfileMoveFeatureTest {
    @Test
    fun `profile move rejects partial movement of a merge group before mutation`() = runTest {
        val source = listOf(entry(1, 7, "/one"), entry(2, 7, "/two"))
        val host = RecordingEntryMergeHost(
            source,
            listOf(EntryMergeMembershipSnapshot(7, 1, listOf(1, 2))),
        )
        val feature = EntryMergeProfileMoveCoordinator(host)
        val prepared = feature.prepare(7, listOf(1))
            .shouldBeInstanceOf<EntryMergeProfileMovePreparationResult.Ready>()
        val inspected = feature.inspectDestination(prepared.reference, 9, emptyList())
            .shouldBeInstanceOf<EntryMergeProfileMoveDestinationResult.Ready>()
        feature.begin(
            EntryMergeProfileMoveIntent(inspected.reference, 9, mapOf(1L to 1L), emptySet()),
        ) shouldBe EntryMergeProfileMoveExecutionResult.Conflict

        host.profileMoveTransitions shouldBe emptyList()
    }
}

private fun entry(id: Long, profileId: Long, url: String): Entry {
    return Entry.create().copy(
        id = id,
        profileId = profileId,
        source = 10,
        url = url,
        title = url,
        favorite = true,
        type = EntryType.BOOK,
    )
}
