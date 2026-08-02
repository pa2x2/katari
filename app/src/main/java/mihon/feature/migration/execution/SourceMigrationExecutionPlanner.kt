package mihon.feature.migration.execution

import mihon.entry.interactions.merge.EntryMergeLibraryGroup
import mihon.entry.interactions.merge.EntryMergeLibraryGroupingFeature
import mihon.feature.migration.execution.model.SourceMigrationExecutionConflict
import mihon.feature.migration.execution.model.SourceMigrationExecutionConflictReason
import mihon.feature.migration.execution.model.SourceMigrationExecutionPlanResult
import mihon.feature.migration.session.model.SourceMigrationSession
import mihon.feature.migration.session.model.SourceMigrationSessionItem
import tachiyomi.domain.entry.repository.EntryRepository

class SourceMigrationExecutionPlanner(
    private val grouping: EntryMergeLibraryGroupingFeature,
    private val entryRepository: EntryRepository,
) {
    suspend fun plan(session: SourceMigrationSession): SourceMigrationExecutionPlanResult {
        val items = session.items.filter(SourceMigrationSessionItem::isIncludedAndReady)
        if (items.isEmpty()) return SourceMigrationExecutionPlanResult.NoItems

        val groups = grouping.groupLibraryEntries(
            profileId = session.profileId,
            entries = entryRepository.getFavoritesByProfile(session.profileId),
        ).groups
        val groupByEntryId = groups
            .flatMap { group -> group.orderedEntries.map { entry -> entry.id to group } }
            .toMap()
        val conflicts = buildList {
            addMissingTargetConflicts(items)
            addTargetDependencyConflicts(items)
            addSharedTargetConflicts(items, groupByEntryId)
            addExternalGroupConflicts(items, groupByEntryId)
        }.distinct()

        return if (conflicts.isEmpty()) {
            SourceMigrationExecutionPlanResult.Ready(items.sortedBy(SourceMigrationSessionItem::position))
        } else {
            SourceMigrationExecutionPlanResult.Conflicted(conflicts)
        }
    }

    private fun MutableList<SourceMigrationExecutionConflict>.addMissingTargetConflicts(
        items: List<SourceMigrationSessionItem>,
    ) {
        items.filter { it.selectedTargetEntryId == null }.forEach { item ->
            add(
                SourceMigrationExecutionConflict(
                    sourceEntryIds = setOf(item.sourceEntryId),
                    reason = SourceMigrationExecutionConflictReason.TARGET_MISSING,
                ),
            )
        }
    }

    private fun MutableList<SourceMigrationExecutionConflict>.addTargetDependencyConflicts(
        items: List<SourceMigrationSessionItem>,
    ) {
        val itemBySourceId = items.associateBy(SourceMigrationSessionItem::sourceEntryId)
        items.forEach { item ->
            val dependent = item.selectedTargetEntryId?.let(itemBySourceId::get) ?: return@forEach
            add(
                SourceMigrationExecutionConflict(
                    sourceEntryIds = setOf(item.sourceEntryId, dependent.sourceEntryId),
                    reason = SourceMigrationExecutionConflictReason.TARGET_IS_ANOTHER_REPLACED_ENTRY,
                ),
            )
        }
    }

    private fun MutableList<SourceMigrationExecutionConflict>.addSharedTargetConflicts(
        items: List<SourceMigrationSessionItem>,
        groupByEntryId: Map<Long, EntryMergeLibraryGroup>,
    ) {
        items.groupBy(SourceMigrationSessionItem::selectedTargetEntryId)
            .filterKeys { it != null }
            .values
            .filter { mappings ->
                mappings.map { mapping -> mapping.sourceGroupKey(groupByEntryId) }.distinct().size > 1
            }
            .forEach { mappings ->
                add(
                    SourceMigrationExecutionConflict(
                        sourceEntryIds = mappings.mapTo(mutableSetOf(), SourceMigrationSessionItem::sourceEntryId),
                        reason = SourceMigrationExecutionConflictReason.SHARED_TARGET_ACROSS_GROUPS,
                    ),
                )
            }
    }

    private fun MutableList<SourceMigrationExecutionConflict>.addExternalGroupConflicts(
        items: List<SourceMigrationSessionItem>,
        groupByEntryId: Map<Long, EntryMergeLibraryGroup>,
    ) {
        val itemBySourceId = items.associateBy(SourceMigrationSessionItem::sourceEntryId)
        val externalTargets = items.mapNotNull { item ->
            val targetId = item.selectedTargetEntryId ?: return@mapNotNull null
            val targetGroup = groupByEntryId[targetId] ?: return@mapNotNull null
            if (targetGroup.groupKey == item.sourceGroupKey(groupByEntryId)) return@mapNotNull null
            ExternalTarget(item, targetGroup)
        }

        externalTargets.forEach { target ->
            val dependentSourceIds = target.group.orderedEntries
                .mapNotNull { entry -> itemBySourceId[entry.id]?.sourceEntryId }
                .toSet()
            if (dependentSourceIds.isNotEmpty()) {
                add(
                    SourceMigrationExecutionConflict(
                        sourceEntryIds = dependentSourceIds + target.item.sourceEntryId,
                        reason = SourceMigrationExecutionConflictReason.OVERLAPPING_EXTERNAL_GROUP,
                    ),
                )
            }
        }

        externalTargets.groupBy { it.group.groupKey }
            .values
            .filter { targets ->
                targets.map { target -> target.item.sourceGroupKey(groupByEntryId) }.distinct().size > 1
            }
            .forEach { targets ->
                add(
                    SourceMigrationExecutionConflict(
                        sourceEntryIds = targets.mapTo(mutableSetOf()) { it.item.sourceEntryId },
                        reason = SourceMigrationExecutionConflictReason.OVERLAPPING_EXTERNAL_GROUP,
                    ),
                )
            }
    }

    private fun SourceMigrationSessionItem.sourceGroupKey(
        groupByEntryId: Map<Long, EntryMergeLibraryGroup>,
    ): Long {
        return groupByEntryId[sourceEntryId]?.groupKey ?: sourceEntryId
    }

    private val EntryMergeLibraryGroup.groupKey: Long
        get() = visibleEntry.id

    private data class ExternalTarget(
        val item: SourceMigrationSessionItem,
        val group: EntryMergeLibraryGroup,
    )
}
