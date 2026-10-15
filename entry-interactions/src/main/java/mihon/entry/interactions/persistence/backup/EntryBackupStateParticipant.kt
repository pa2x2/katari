package mihon.entry.interactions.persistence.backup

/**
 * One feature's state carried inside every backed-up entry. [stateId] is written into backups, so it must never
 * change. A participant skips types its feature does not support; the feature already guards them.
 */
internal interface EntryBackupStateParticipant {
    val stateId: String

    suspend fun snapshot(event: EntryBackupSnapshotEvent)

    suspend fun restore(event: EntryBackupRestoreEvent)

    /** Runs once per restored type after every entry of the session is restored. */
    suspend fun finalizeRestore(event: EntryBackupRestoreFinalizingEvent) = Unit
}
