package mihon.entry.interactions.merge.backup

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.merge.EntryMergeBackupFeature
import mihon.entry.interactions.merge.EntryMergeBackupGroup
import mihon.entry.interactions.merge.EntryMergeBackupGroupMember
import mihon.entry.interactions.merge.EntryMergeBackupIdentity
import mihon.entry.interactions.merge.EntryMergeBackupMember
import mihon.entry.interactions.merge.EntryMergeBackupRestoreResult
import mihon.entry.interactions.merge.EntryMergeSubject
import mihon.entry.interactions.persistence.backup.EntryBackupRestoreEvent
import mihon.entry.interactions.persistence.backup.EntryBackupRestoreFinalizingEvent
import mihon.entry.interactions.persistence.backup.EntryBackupRestoreIssue
import mihon.entry.interactions.persistence.backup.EntryBackupRestoreIssueSink
import mihon.entry.interactions.persistence.backup.EntryBackupRestoreSession
import mihon.entry.interactions.persistence.backup.EntryBackupRestoreSessionId
import mihon.entry.interactions.persistence.backup.EntryBackupRestoreStateSource
import mihon.entry.interactions.persistence.backup.EntryFeatureStateEnvelope
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry

class EntryMergeBackupRestoreParticipationTest {

    @Test
    fun `deferred groups belong to the restore session that captured them`() = runTest {
        val feature = RecordingMergeBackupFeature()
        val participation =
            EntryMergeBackupRestoreParticipation(
                feature,
            )
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

    @Test
    fun `a merge restored onto an install with other source keys keeps its target's source`() = runTest {
        val feature = RecordingMergeBackupFeature()
        val participation = EntryMergeBackupRestoreParticipation(feature)
        // The backup's source -2 has key 7 here, and the backup's key 7 belongs to another source, which has key 9
        val session = session("restore", mapOf(-2L to 7L, 7L to 9L))
        val target = Entry.create().copy(source = 7, url = "/target", type = EntryType.BOOK)
        val member = Entry.create().copy(source = 7, url = "/member", type = EntryType.BOOK)
        val backedUpTarget = EntryMergeBackupIdentity(-2, "/target", EntryType.BOOK)

        participation.enqueue(restoreEvent(session, target), EntryMergeBackupMember(backedUpTarget, position = 0))
        participation.enqueue(restoreEvent(session, member), EntryMergeBackupMember(backedUpTarget, position = 1))
        participation.finalize(finalizingEvent(session))

        feature.restoredGroups.single().single() shouldBe EntryMergeBackupGroup(
            target = EntryMergeBackupIdentity(7, "/target", EntryType.BOOK),
            members = listOf(
                EntryMergeBackupGroupMember(EntryMergeBackupIdentity(7, "/target", EntryType.BOOK), position = 0),
                EntryMergeBackupGroupMember(EntryMergeBackupIdentity(7, "/member", EntryType.BOOK), position = 1),
            ),
        )
    }
}

private fun session(id: String, localKeys: Map<Long, Long> = emptyMap()) = EntryBackupRestoreSession(
    EntryBackupRestoreSessionId(id),
    sourceKeys = { localKeys[it] ?: it },
)

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

    override suspend fun companionsForBackup(profileId: Long, entries: List<Entry>): List<Entry> = emptyList()

    override suspend fun snapshotForBackup(subject: EntryMergeSubject): EntryMergeBackupMember? = null

    override suspend fun restore(
        destinationProfileId: Long,
        groups: List<EntryMergeBackupGroup>,
    ): EntryMergeBackupRestoreResult {
        restoredGroups += groups
        return EntryMergeBackupRestoreResult(groups.size, emptyList())
    }
}
