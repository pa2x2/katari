package mihon.entry.interactions

@JvmInline
value class EntryBackupRestoreSessionId(val value: String) {
    init {
        require(value.isNotBlank()) { "Backup restore session ID cannot be blank" }
    }
}

/** Per-restore state whose lifetime is owned by the caller rather than the application composition. */
class EntryBackupRestoreSession(
    val id: EntryBackupRestoreSessionId,
) {
    private val participantStates = linkedMapOf<EntryBackupRestoreSessionStateKey<*>, Any>()

    fun <T : Any> state(
        key: EntryBackupRestoreSessionStateKey<T>,
        create: () -> T,
    ): T = synchronized(participantStates) {
        @Suppress("UNCHECKED_CAST")
        participantStates.getOrPut(key, create) as T
    }

    fun <T : Any> stateOrNull(key: EntryBackupRestoreSessionStateKey<T>): T? = synchronized(participantStates) {
        @Suppress("UNCHECKED_CAST")
        participantStates[key] as T?
    }

    fun <T : Any> remove(key: EntryBackupRestoreSessionStateKey<T>): T? = synchronized(participantStates) {
        @Suppress("UNCHECKED_CAST")
        participantStates.remove(key) as T?
    }
}

/** Participant-owned identity for state that must span multiple Entry restore events. */
class EntryBackupRestoreSessionStateKey<T : Any>(val value: String) {
    init {
        require(value.isNotBlank()) { "Backup restore session state key cannot be blank" }
    }
}
