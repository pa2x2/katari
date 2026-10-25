package mihon.entry.interactions.persistence.backup

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry

internal class EntryBackupCoordinator(
    participants: Set<EntryBackupStateParticipant>,
) : EntryBackupFeature {
    // Ordered by state id so every backup lists its states in the same order.
    private val participants = participants.sortedBy(EntryBackupStateParticipant::stateId)

    override suspend fun withCompanionEntries(profileId: Long, entries: List<Entry>): List<Entry> {
        val included = entries.distinctBy(Entry::id).toMutableList()
        val includedIds = included.mapTo(mutableSetOf(), Entry::id)
        // A companion can need companions of its own, such as a merged entry's listed titles and their merges.
        var added: List<Entry> = included.toList()
        while (added.isNotEmpty()) {
            val batch = added
            added = participants.flatMap { it.companionEntries(profileId, batch) }.filter { includedIds.add(it.id) }
            included += added
        }
        return included
    }

    override suspend fun snapshot(
        profileId: Long,
        entry: Entry,
        selection: EntryBackupSelection,
    ): List<EntryFeatureStateEnvelope> {
        val states = MutableEntryBackupStates()
        val event = EntryBackupSnapshotEvent(profileId, entry, selection, states)
        participants.forEach { it.snapshot(event) }
        return states.values()
    }

    override suspend fun restore(
        session: EntryBackupRestoreSession,
        profileId: Long,
        entry: Entry,
        states: List<EntryFeatureStateEnvelope>,
    ) {
        val event = EntryBackupRestoreEvent(session, profileId, entry, ImmutableEntryBackupStates(states))
        participants.forEach { it.restore(event) }
    }

    override suspend fun finalizeRestore(
        session: EntryBackupRestoreSession,
        profileId: Long,
        restoredTypes: Set<EntryType>,
    ): EntryBackupRestoreFinalization {
        val issues = MutableEntryBackupRestoreIssues()
        for (type in restoredTypes) {
            val event = EntryBackupRestoreFinalizingEvent(session, profileId, type, issues)
            participants.forEach { it.finalizeRestore(event) }
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
