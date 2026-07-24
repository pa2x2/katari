package mihon.entry.interactions

import mihon.feature.graph.FeatureExecutionResult
import mihon.feature.graph.FeatureExecutionRuntime
import tachiyomi.domain.entry.model.Entry

internal class EntryBackupCoordinator(
    private val executions: FeatureExecutionRuntime,
) : EntryBackupFeature {
    override suspend fun snapshot(
        profileId: Long,
        entry: Entry,
        selection: EntryBackupSelection,
    ): List<EntryFeatureStateEnvelope> {
        val states = MutableEntryBackupStates()
        executions.executeInline(
            point = ENTRY_BACKUP_SNAPSHOT_EXECUTION_POINT,
            contentType = entry.type.toContentTypeId(),
            event = EntryBackupSnapshotEvent(profileId, entry, selection, states),
        ).requireSuccessful("snapshot")
        return states.values()
    }

    override suspend fun restore(
        session: EntryBackupRestoreSession,
        profileId: Long,
        entry: Entry,
        states: List<EntryFeatureStateEnvelope>,
    ) {
        val source = ImmutableEntryBackupStates(states)
        executions.executeInline(
            point = ENTRY_BACKUP_RESTORE_EXECUTION_POINT,
            contentType = entry.type.toContentTypeId(),
            event = EntryBackupRestoreEvent(session, profileId, entry, source),
        ).requireSuccessful("restore")
    }

    override suspend fun finalizeRestore(
        session: EntryBackupRestoreSession,
        profileId: Long,
        restoredTypes: Set<eu.kanade.tachiyomi.source.entry.EntryType>,
    ): EntryBackupRestoreFinalization {
        val issues = MutableEntryBackupRestoreIssues()
        // Execute only for types actually restored in this session/profile. This remains open to future Entry types.
        for (type in restoredTypes) {
            executions.executeInline(
                point = ENTRY_BACKUP_RESTORE_FINALIZING_EXECUTION_POINT,
                contentType = type.toContentTypeId(),
                event = EntryBackupRestoreFinalizingEvent(session, profileId, type, issues),
            ).requireSuccessful("restore finalization")
        }
        return EntryBackupRestoreFinalization(issues.values())
    }
}

private class MutableEntryBackupStates : EntryBackupSnapshotContributionSink {
    private val states = linkedMapOf<String, EntryFeatureStateEnvelope>()

    override fun add(state: EntryFeatureStateEnvelope) {
        require(state.participantId.isNotBlank()) { "Backup state participant ID cannot be blank" }
        require(state.schemaVersion > 0) { "Backup state schema version must be positive" }
        check(states.putIfAbsent(state.participantId, state) == null) {
            "Duplicate backup state from participant ${state.participantId}"
        }
    }

    fun values(): List<EntryFeatureStateEnvelope> = states.values.toList()
}

private class ImmutableEntryBackupStates(states: List<EntryFeatureStateEnvelope>) : EntryBackupRestoreStateSource {
    private val statesByParticipant = states.associateByUnique(EntryFeatureStateEnvelope::participantId)

    init {
        states.forEach { state ->
            require(state.participantId.isNotBlank()) { "Backup state participant ID cannot be blank" }
            require(state.schemaVersion > 0) { "Backup state schema version must be positive" }
        }
    }

    override fun state(participantId: String): EntryFeatureStateEnvelope? = statesByParticipant[participantId]
}

private class MutableEntryBackupRestoreIssues : EntryBackupRestoreIssueSink {
    private val issues = mutableListOf<EntryBackupRestoreIssue>()

    override fun add(issue: EntryBackupRestoreIssue) {
        require(issue.participantId.isNotBlank()) { "Backup restore issue participant ID cannot be blank" }
        issues += issue
    }

    fun values(): List<EntryBackupRestoreIssue> = issues.toList()
}

private fun <T, K> List<T>.associateByUnique(keySelector: (T) -> K): Map<K, T> {
    val result = linkedMapOf<K, T>()
    forEach { value ->
        val key = keySelector(value)
        check(result.putIfAbsent(key, value) == null) { "Duplicate backup state from participant $key" }
    }
    return result
}

private fun FeatureExecutionResult.requireSuccessful(operation: String) {
    check(isSuccessful) {
        "Entry backup $operation participants failed: ${failures.joinToString { it.participant.value }}"
    }
}
