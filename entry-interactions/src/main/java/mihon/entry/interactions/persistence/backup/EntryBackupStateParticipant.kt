package mihon.entry.interactions.persistence.backup

import tachiyomi.domain.entry.model.Entry

/**
 * One feature's state carried inside every backed-up entry. [stateId] is written into backups, so it must never
 * change. A participant skips types its feature does not support; the feature already guards them.
 */
internal interface EntryBackupStateParticipant {
    val stateId: String

    /**
     * Entries missing from [entries] that a backup holding them must also hold, because this feature's state about
     * them, such as a merge, only restores whole.
     */
    suspend fun companionEntries(profileId: Long, entries: List<Entry>): List<Entry> = emptyList()

    suspend fun snapshot(event: EntryBackupSnapshotEvent)

    suspend fun restore(event: EntryBackupRestoreEvent)

    /** Runs once per restored type after every entry of the session is restored. */
    suspend fun finalizeRestore(event: EntryBackupRestoreFinalizingEvent) = Unit
}
