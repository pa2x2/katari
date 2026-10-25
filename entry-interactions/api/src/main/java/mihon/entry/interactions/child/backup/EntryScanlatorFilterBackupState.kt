package mihon.entry.interactions.child.backup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The ID and field name are read back from existing backups, so they keep the filter's former "child group" name
const val ENTRY_SCANLATOR_FILTER_BACKUP_STATE_ID = "entry.child-group-filter.backup"
const val ENTRY_SCANLATOR_FILTER_BACKUP_SCHEMA_VERSION = 1

@Serializable
data class EntryScanlatorFilterBackupState(
    @SerialName("excludedGroups")
    val excludedScanlators: Set<String>,
)
