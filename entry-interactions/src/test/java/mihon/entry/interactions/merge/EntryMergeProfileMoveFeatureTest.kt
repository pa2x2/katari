package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.host.EntryMergeMembershipSnapshot
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry

class EntryMergeProfileMoveFeatureTest {
    @Test
    fun `profile move reference expands source groups and binds destination membership`() = runTest {
        val source = listOf(entry(1, 7, "/one"), entry(2, 7, "/two"))
        val destination = entry(20, 9, "/duplicate")
        val destinationMember = entry(21, 9, "/other")
        val host = RecordingEntryMergeHost(
            entries = source + destination + destinationMember,
            memberships = listOf(
                EntryMergeMembershipSnapshot(7, 1, listOf(1, 2)),
                EntryMergeMembershipSnapshot(9, 20, listOf(20, 21)),
            ),
        )
        val feature = EntryMergeProfileMoveCoordinator(host)
        val prepared = feature.prepare(7, listOf(2))
            .shouldBeInstanceOf<EntryMergeProfileMovePreparationResult.Ready>()
        prepared.units.single().entries.map(Entry::id) shouldContainExactly listOf(1, 2)
        val inspected = feature.inspectDestination(prepared.reference, 9, listOf(20))
            .shouldBeInstanceOf<EntryMergeProfileMoveDestinationResult.Ready>()
        inspected.mergeAffectedEntryIds shouldBe setOf(20L)

        val intent =
            EntryMergeProfileMoveIntent(
                reference = inspected.reference,
                destinationProfileId = 9,
                destinationEntryIdsBySourceEntryId = mapOf(1L to 1L, 2L to 2L),
                destinationEntryIdsToDetach = setOf(20),
            )
        feature.begin(intent) shouldBe EntryMergeProfileMoveExecutionResult.Applied
        feature.complete(intent) shouldBe EntryMergeProfileMoveExecutionResult.Applied

        host.profileMoveTransitions.single().run {
            expectedSourceGroups.single().orderedEntryIds shouldContainExactly listOf(1, 2)
            expectedDestinationGroups.single().orderedEntryIds shouldContainExactly listOf(20, 21)
        }
    }

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

    @Test
    fun `profile move can skip one complete group while moving another unit`() = runTest {
        val source = listOf(entry(1, 7, "/one"), entry(2, 7, "/two"), entry(3, 7, "/standalone"))
        val host = RecordingEntryMergeHost(
            source,
            listOf(EntryMergeMembershipSnapshot(7, 1, listOf(1, 2))),
        )
        val feature = EntryMergeProfileMoveCoordinator(host)
        val prepared = feature.prepare(7, listOf(1, 3))
            .shouldBeInstanceOf<EntryMergeProfileMovePreparationResult.Ready>()
        val inspected = feature.inspectDestination(prepared.reference, 9, emptyList())
            .shouldBeInstanceOf<EntryMergeProfileMoveDestinationResult.Ready>()

        val intent = EntryMergeProfileMoveIntent(inspected.reference, 9, mapOf(3L to 3L), emptySet())
        feature.begin(intent) shouldBe EntryMergeProfileMoveExecutionResult.Applied
        feature.complete(intent) shouldBe EntryMergeProfileMoveExecutionResult.Applied

        host.profileMoveTransitions.single().run {
            expectedSourceEntries.map(Entry::id) shouldContainExactly listOf(3L)
            expectedSourceGroups shouldBe emptyList()
            expectedStandaloneEntryIds shouldBe setOf(3L)
        }
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
