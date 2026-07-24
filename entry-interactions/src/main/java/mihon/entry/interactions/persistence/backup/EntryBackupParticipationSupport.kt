package mihon.entry.interactions

import kotlinx.serialization.KSerializer

internal fun <T> entryBackupStateEnvelope(
    participantId: String,
    schemaVersion: Int,
    serializer: KSerializer<T>,
    value: T,
): EntryFeatureStateEnvelope {
    return EntryFeatureStateEnvelope(
        participantId = participantId,
        schemaVersion = schemaVersion,
        payload = EntryBackupStateCodec.encode(serializer, value),
    )
}

internal fun <T> EntryBackupRestoreStateSource.decodeEntryBackupState(
    participantId: String,
    supportedSchemaVersion: Int,
    serializer: KSerializer<T>,
): T? {
    val state = state(participantId) ?: return null
    require(state.schemaVersion == supportedSchemaVersion) {
        "Unsupported $participantId backup state schema ${state.schemaVersion}"
    }
    return EntryBackupStateCodec.decode(serializer, state.payload)
}
