package mihon.entry.interactions.media.backup

import mihon.entry.interactions.media.EntryViewerSettingsFeature
import mihon.entry.interactions.media.EntryViewerSettingsSnapshotResult
import mihon.entry.interactions.persistence.backup.EntryBackupRestoreEvent
import mihon.entry.interactions.persistence.backup.EntryBackupSnapshotEvent
import mihon.entry.interactions.persistence.backup.EntryBackupStateParticipant
import mihon.entry.interactions.persistence.backup.decodeEntryBackupState
import mihon.entry.interactions.persistence.backup.entryBackupStateEnvelope
import mihon.entry.viewer.settings.ViewerSettingId
import mihon.entry.viewer.settings.ViewerSettingOverride

internal class EntryViewerSettingsBackupParticipant(
    private val feature: () -> EntryViewerSettingsFeature,
) : EntryBackupStateParticipant {
    override val stateId = ENTRY_VIEWER_SETTINGS_BACKUP_STATE_ID

    override suspend fun snapshot(event: EntryBackupSnapshotEvent) {
        val results = feature().snapshot(event.entries)
        event.entries.forEach { entry ->
            val result = results[entry.id]
            if (result !is EntryViewerSettingsSnapshotResult.Available || result.overrides.isEmpty()) return@forEach
            val state = EntryViewerSettingsBackupState(
                result.overrides.map { override ->
                    EntryViewerSettingBackupValue(
                        providerId = override.settingId.providerId,
                        settingKey = override.settingId.key,
                        encodedValue = override.encodedValue,
                        updatedAt = override.updatedAt,
                    )
                },
            )
            event.contributions.add(
                entry,
                entryBackupStateEnvelope(
                    stateId,
                    ENTRY_VIEWER_SETTINGS_BACKUP_SCHEMA_VERSION,
                    EntryViewerSettingsBackupState.serializer(),
                    state,
                ),
            )
        }
    }

    override suspend fun restore(event: EntryBackupRestoreEvent) {
        val state = event.states.decodeEntryBackupState(
            stateId,
            ENTRY_VIEWER_SETTINGS_BACKUP_SCHEMA_VERSION,
            EntryViewerSettingsBackupState.serializer(),
        ) ?: return
        feature().restore(
            event.entry,
            state.overrides.map { value ->
                ViewerSettingOverride(
                    entryId = event.entry.id,
                    settingId = ViewerSettingId(value.providerId, value.settingKey),
                    encodedValue = value.encodedValue,
                    updatedAt = value.updatedAt,
                )
            },
        )
    }
}
