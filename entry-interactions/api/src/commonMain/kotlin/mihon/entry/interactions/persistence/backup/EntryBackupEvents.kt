package mihon.entry.interactions.persistence.backup

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry

/**
 * A chunk of a profile's entries being backed up together, so a participant can read all of their state at once
 * instead of querying once per entry.
 */
data class EntryBackupSnapshotEvent(
    val profileId: Long,
    val entries: List<Entry>,
    val selection: EntryBackupSelection,
    val contributions: EntryBackupSnapshotContributionSink,
)

interface EntryBackupSnapshotContributionSink {
    fun add(entry: Entry, state: EntryFeatureStateEnvelope)
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
