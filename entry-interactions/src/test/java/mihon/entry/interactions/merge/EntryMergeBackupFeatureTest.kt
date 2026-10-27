package mihon.entry.interactions.merge

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.merge.host.EntryMergeHostTransition
import mihon.entry.interactions.merge.host.EntryMergeMembershipSnapshot
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry

class EntryMergeBackupFeatureTest {
    @Test
    fun `malformed restore group is skipped without a persistence transition`() = runTest {
        val target = EntryMergeBackupIdentity(10, "/target", EntryType.BOOK)
        val host = RecordingEntryMergeHost(emptyList())

        val result = EntryMergeBackupCoordinator(
            host,
        ).restore(
            destinationProfileId = 9,
            groups = listOf(
                EntryMergeBackupGroup(
                    target,
                    listOf(
                        EntryMergeBackupGroupMember(
                            EntryMergeBackupIdentity(10, "/anime", EntryType.ANIME),
                            1,
                        ),
                    ),
                ),
            ),
        )

        result.skippedGroups.single().reason shouldBe EntryMergeBackupSkipReason.MIXED_ENTRY_TYPES
        host.transitions shouldBe emptyList()
    }

    @Test
    fun `restoring a backup that lacks some members of an existing merge keeps them after the backed-up ones`() =
        runTest {
            val entries = (1L..4L).map { id ->
                Entry.create().copy(id = id, profileId = 9, source = 10, url = "/$id", type = EntryType.ANIME)
            }
            val existing = EntryMergeMembershipSnapshot(9, targetEntryId = 1, orderedEntryIds = listOf(1, 2, 3, 4))
            val host = RecordingEntryMergeHost(entries, listOf(existing))

            EntryMergeBackupCoordinator(host).restore(
                destinationProfileId = 9,
                groups = listOf(
                    EntryMergeBackupGroup(
                        entries[0].backupIdentity(),
                        listOf(
                            EntryMergeBackupGroupMember(entries[0].backupIdentity(), 0),
                            EntryMergeBackupGroupMember(entries[2].backupIdentity(), 1),
                        ),
                    ),
                ),
            )

            val transition = host.transitions.single() as EntryMergeHostTransition.RestoreBackupGroup
            transition.expectedGroups shouldBe listOf(existing)
            transition.orderedEntryIds shouldBe listOf(1L, 3L, 2L, 4L)
        }
}
