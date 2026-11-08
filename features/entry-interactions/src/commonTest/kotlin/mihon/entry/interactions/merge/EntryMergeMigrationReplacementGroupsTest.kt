package mihon.entry.interactions.merge

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.merge.host.EntryMergeHostTransition
import mihon.entry.interactions.merge.host.EntryMergeMembershipSnapshot
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry

class EntryMergeMigrationReplacementGroupsTest {
    @Test
    fun `replacement transfers one member and dissolves a depleted source group`() {
        val replacements = replacementGroups(
            currentEntryId = 2L,
            replacementEntryId = 3L,
            currentGroup = EntryMergeMembershipSnapshot(7L, 1L, listOf(1L, 2L)),
            replacementGroup = EntryMergeMembershipSnapshot(7L, 3L, listOf(3L, 4L)),
        )

        replacements shouldContainExactly listOf(
            EntryMergeMembershipSnapshot(7L, 1L, listOf(1L, 3L)),
        )
    }

    /** The seasons a merged entry's source supplied used to stay merged under an entry that lists them all itself. */
    @Test
    fun `a replaced merged entry keeps its source's members and hands on only the ones a user added`() = runTest {
        val root = entry(ROOT, mergedBySource = true)
        val replacement = entry(REPLACEMENT)
        val host = RecordingEntryMergeHost(
            entries = listOf(root, entry(SEASON_2), entry(ADDED), entry(SEASON_3), replacement),
            memberships = listOf(
                EntryMergeMembershipSnapshot(PROFILE_ID, ROOT, listOf(ROOT, SEASON_2, ADDED, SEASON_3)),
            ),
            sourceListings = mapOf(ROOT to listOf(SEASON_2, SEASON_3)),
        )

        EntryMergeMigrationCoordinator(host)
            .participateInReplacementTransaction(EntryMergeMigrationReplacementIntent(root, replacement))

        val transition = host.transitions.single() as EntryMergeHostTransition.ReplaceForMigration
        transition.replacementGroups shouldContainExactlyInAnyOrder listOf(
            EntryMergeMembershipSnapshot(PROFILE_ID, REPLACEMENT, listOf(REPLACEMENT, ADDED)),
            EntryMergeMembershipSnapshot(PROFILE_ID, ROOT, listOf(ROOT, SEASON_2, SEASON_3)),
        )
        transition.libraryRemovalEntryIds shouldBe setOf(SEASON_2, SEASON_3)
    }

    private fun entry(id: Long, mergedBySource: Boolean = false) = Entry.create().copy(
        id = id,
        profileId = PROFILE_ID,
        favorite = true,
        mergedBySource = mergedBySource,
        type = EntryType.ANIME,
    )

    private companion object {
        const val PROFILE_ID = 1L
        const val ROOT = 1L
        const val SEASON_2 = 2L
        const val ADDED = 3L
        const val SEASON_3 = 4L
        const val REPLACEMENT = 5L
    }
}
