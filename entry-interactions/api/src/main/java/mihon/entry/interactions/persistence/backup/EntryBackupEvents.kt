package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry

data class EntryBackupSnapshotEvent(
    val profileId: Long,
    val entry: Entry,
    val selection: EntryBackupSelection,
    val contributions: EntryBackupSnapshotContributionSink,
)

interface EntryBackupSnapshotContributionSink {
    fun add(state: EntryFeatureStateEnvelope)
}

data class EntryBackupRestoreEvent(
    val session: EntryBackupRestoreSession,
    val profileId: Long,
    val entry: Entry,
    val states: EntryBackupRestoreStateSource,
)

interface EntryBackupRestoreStateSource {
    fun state(participantId: String): EntryFeatureStateEnvelope?
}

data class EntryBackupRestoreFinalizingEvent(
    val session: EntryBackupRestoreSession,
    val profileId: Long,
    val type: EntryType,
    val issues: EntryBackupRestoreIssueSink,
)

interface EntryBackupRestoreIssueSink {
    fun add(issue: EntryBackupRestoreIssue)
}
