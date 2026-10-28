package mihon.feature.migration.session.model

import tachiyomi.domain.entry.model.Entry

data class SourceMigrationSessionGroup(
    val sessionId: SourceMigrationSessionId,
    val groupId: Long,
    val position: Long,
    val visibleEntryId: Long,
    val visibleTitle: String,
    val members: List<SourceMigrationSessionGroupMember>,
)

data class SourceMigrationSessionGroupMember(
    val entryId: Long,
    val position: Long,
    val sourceId: Long,
    val title: String,
    val url: String,
    val thumbnailUrl: String?,
    val selected: Boolean,
)

data class SourceMigrationSessionGroupDraft(
    val visibleEntry: Entry,
    val members: List<Entry>,
    val selectedEntryIds: Set<Long>,
) {
    init {
        require(members.isNotEmpty()) { "Source Migration group requires at least one member" }
        require(members.map(Entry::id).distinct().size == members.size) {
            "Source Migration group members must be unique"
        }
        require(visibleEntry.id in members.map(Entry::id)) {
            "Source Migration visible Entry must belong to its group"
        }
        require(selectedEntryIds.isNotEmpty()) { "Source Migration group requires a selected member" }
        require(members.mapTo(mutableSetOf(), Entry::id).containsAll(selectedEntryIds)) {
            "Source Migration selected Entries must belong to their group"
        }
    }
}
