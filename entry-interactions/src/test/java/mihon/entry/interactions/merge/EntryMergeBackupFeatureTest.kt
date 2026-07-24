package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.host.EntryMergeHostTransition
import mihon.entry.interactions.host.EntryMergeMembershipSnapshot
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry

class EntryMergeBackupFeatureTest {
    @Test
    fun `backup snapshot exposes only stable target identity and member position`() = runTest {
        val entries = listOf(entry(1, 7, "/target"), entry(2, 7, "/member"))
        val host = RecordingEntryMergeHost(entries, listOf(EntryMergeMembershipSnapshot(7, 1, listOf(1, 2))))

        EntryMergeBackupCoordinator(host).snapshotForBackup(EntryMergeSubject(7, 2)) shouldBe
            EntryMergeBackupMember(EntryMergeBackupIdentity(10, "/target", EntryType.BOOK), 1)
    }

    @Test
    fun `restore resolves portable identities and submits one owned group transition`() = runTest {
        val entries = listOf(entry(11, 9, "/target"), entry(12, 9, "/member"))
        val host = RecordingEntryMergeHost(entries)
        val target = EntryMergeBackupIdentity(10, "/target", EntryType.BOOK)

        val result = EntryMergeBackupCoordinator(host).restore(
            destinationProfileId = 9,
            groups = listOf(
                EntryMergeBackupGroup(
                    target,
                    listOf(
                        EntryMergeBackupGroupMember(target, 0),
                        EntryMergeBackupGroupMember(EntryMergeBackupIdentity(10, "/member", EntryType.BOOK), 1),
                    ),
                ),
            ),
        )

        result shouldBe EntryMergeBackupRestoreResult(1, emptyList())
        host.transitions.single().shouldBeInstanceOf<EntryMergeHostTransition.RestoreBackupGroup>().run {
            targetEntryId shouldBe 11
            orderedEntryIds shouldContainExactly listOf(11, 12)
        }
    }

    @Test
    fun `malformed restore group is skipped without a persistence transition`() = runTest {
        val target = EntryMergeBackupIdentity(10, "/target", EntryType.BOOK)
        val host = RecordingEntryMergeHost(emptyList())

        val result = EntryMergeBackupCoordinator(host).restore(
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
