package eu.kanade.tachiyomi.ui.browse.migration.entry.models

import androidx.compose.runtime.Immutable
import tachiyomi.domain.entry.model.Entry

@Immutable
data class MigrationEntrySelectionGroup(
    val visibleEntry: Entry,
    val members: List<MigrationEntrySelectionMember>,
    val totalMemberCount: Int = members.size,
) {
    val key: Long
        get() = visibleEntry.id

    val rootEntryId: Long
        get() = visibleEntry.id

    val isMerged: Boolean
        get() = totalMemberCount > 1

    val eligibleMembers: List<MigrationEntrySelectionMember>
        get() = members.filter { it.availability == MigrationEntrySelectionAvailability.ELIGIBLE }

    val eligibleEntryIds: Set<Long>
        get() = eligibleMembers.mapTo(linkedSetOf()) { it.entry.id }
}

@Immutable
data class MigrationEntrySelectionMember(
    val entry: Entry,
    val sourceName: String,
    val availability: MigrationEntrySelectionAvailability,
    val progress: MigrationEntrySelectionProgress = MigrationEntrySelectionProgress(),
)

enum class MigrationEntrySelectionAvailability {
    ELIGIBLE,
    OTHER_SOURCE,
    UNAVAILABLE,
}

@Immutable
data class MigrationEntrySelectionProgress(
    val consumedCount: Int = 0,
    val totalCount: Int = 0,
)
