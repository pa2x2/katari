package mihon.feature.migration.session.model

import mihon.entry.interactions.migration.EntryMigrationOption
import tachiyomi.domain.entry.model.Entry

data class SourceMigrationSessionDraft(
    val profileId: Long,
    val originSourceId: Long,
    val groups: List<SourceMigrationSessionGroupDraft>,
    val targetSourceIds: List<Long>,
    val selectedOptions: Set<EntryMigrationOption>,
) {
    val entries = groups.flatMap { group ->
        group.members.filter { it.id in group.selectedEntryIds }
    }

    init {
        require(groups.isNotEmpty()) { "Source Migration session requires at least one group" }
        require(groups.map { it.visibleEntry.id }.distinct().size == groups.size) {
            "Source Migration session groups must be unique"
        }
        require(entries.isNotEmpty()) { "Source Migration session requires at least one Entry" }
        require(entries.all { it.profileId == profileId }) {
            "Source Migration session Entries must belong to its profile"
        }
        require(entries.all { it.source == originSourceId }) {
            "Source Migration session Entries must belong to its origin source"
        }
        require(entries.map(Entry::id).distinct().size == entries.size) {
            "Source Migration session Entries must be unique"
        }
        require(targetSourceIds.isNotEmpty()) { "Source Migration session requires target sources" }
        require(targetSourceIds.distinct().size == targetSourceIds.size) {
            "Source Migration target sources must be unique"
        }
        require(originSourceId !in targetSourceIds) {
            "Source Migration origin source cannot be a target source"
        }
        require(groups.flatMap(SourceMigrationSessionGroupDraft::members).all { it.profileId == profileId }) {
            "Source Migration group members must belong to its profile"
        }
    }
}
