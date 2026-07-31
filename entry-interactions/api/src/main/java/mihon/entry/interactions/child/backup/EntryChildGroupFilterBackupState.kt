package mihon.entry.interactions.child.backup

import kotlinx.serialization.Serializable

const val ENTRY_CHILD_GROUP_FILTER_BACKUP_STATE_ID = "entry.child-group-filter.backup"
const val ENTRY_CHILD_GROUP_FILTER_BACKUP_SCHEMA_VERSION = 1

@Serializable
data class EntryChildGroupFilterBackupState(
    val excludedGroups: Set<String>,
)
