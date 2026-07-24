package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry

class EntryMergeBackupRestoreParticipationTest {

    @Test
    fun `deferred groups belong to the restore session that captured them`() = runTest {
        val feature = RecordingMergeBackupFeature()
        val participation = EntryMergeBackupRestoreParticipation(feature)
        val capturedSession = session("captured")
        val unrelatedSession = session("unrelated")
        val target = EntryMergeBackupIdentity(10, "/target", EntryType.BOOK)
        val entry = Entry.create().copy(source = 10, url = "/member", type = EntryType.BOOK)

        participation.enqueue(
            restoreEvent(capturedSession, entry),
            EntryMergeBackupMember(target, position = 1),
        )

        participation.finalize(finalizingEvent(unrelatedSession))
        feature.restoredGroups shouldBe emptyList()

        participation.finalize(finalizingEvent(capturedSession))
        feature.restoredGroups.shouldHaveSize(1)
        feature.restoredGroups.single().single() shouldBe EntryMergeBackupGroup(
            target = target,
            members = listOf(
                EntryMergeBackupGroupMember(
                    identity = EntryMergeBackupIdentity(10, "/member", EntryType.BOOK),
                    position = 1,
                ),
            ),
        )
    }
}

private fun session(id: String) = EntryBackupRestoreSession(EntryBackupRestoreSessionId(id))

private fun restoreEvent(session: EntryBackupRestoreSession, entry: Entry) = EntryBackupRestoreEvent(
    session = session,
    profileId = 4,
    entry = entry,
    states = object : EntryBackupRestoreStateSource {
        override fun state(participantId: String): EntryFeatureStateEnvelope? = null
    },
)

private fun finalizingEvent(session: EntryBackupRestoreSession) = EntryBackupRestoreFinalizingEvent(
    session = session,
    profileId = 4,
    type = EntryType.BOOK,
    issues = object : EntryBackupRestoreIssueSink {
        override fun add(issue: EntryBackupRestoreIssue) = Unit
    },
)

private class RecordingMergeBackupFeature : EntryMergeBackupFeature {
    val restoredGroups = mutableListOf<List<EntryMergeBackupGroup>>()

    override suspend fun snapshotForBackup(subject: EntryMergeSubject): EntryMergeBackupMember? = null

    override suspend fun restore(
        destinationProfileId: Long,
        groups: List<EntryMergeBackupGroup>,
    ): EntryMergeBackupRestoreResult {
        restoredGroups += groups
        return EntryMergeBackupRestoreResult(groups.size, emptyList())
    }
}
