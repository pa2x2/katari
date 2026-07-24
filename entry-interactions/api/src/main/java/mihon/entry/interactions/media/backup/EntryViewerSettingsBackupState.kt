package mihon.entry.interactions

import kotlinx.serialization.Serializable

const val ENTRY_VIEWER_SETTINGS_BACKUP_STATE_ID = "entry.viewer-settings.backup"
const val ENTRY_VIEWER_SETTINGS_BACKUP_SCHEMA_VERSION = 1

@Serializable
data class EntryViewerSettingsBackupState(
    val overrides: List<EntryViewerSettingBackupValue>,
)

@Serializable
data class EntryViewerSettingBackupValue(
    val providerId: String,
    val settingKey: String,
    val encodedValue: String,
    val updatedAt: Long,
)
