package mihon.entry.interactions.merge

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

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
}
