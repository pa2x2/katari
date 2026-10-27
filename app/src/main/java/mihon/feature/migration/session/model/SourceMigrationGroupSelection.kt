package mihon.feature.migration.session.model

import java.io.Serializable

/**
 * The ids behind a [SourceMigrationSessionGroupDraft], which is what migration screens keep in saved state: a draft
 * holds whole Entries, which can overflow the saved-state transaction for a large source.
 */
data class SourceMigrationGroupSelection(
    val visibleEntryId: Long,
    val memberIds: List<Long>,
    val selectedEntryIds: Set<Long>,
) : Serializable
