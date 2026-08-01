package mihon.feature.migration.list.models

data class MigrationMergeContext(
    val rootEntryId: Long,
    val rootTitle: String,
    val memberCount: Int,
    val isRoot: Boolean,
)

data class MigrationMergeImpactSummary(
    val hasAnyMergedEntries: Boolean,
    val hasMergedRoots: Boolean,
    val hasMergedMembers: Boolean,
    val hasSameGroupTargets: Boolean,
    val hasOtherGroupTargets: Boolean,
    val hasStandaloneSourcesWithMergedTargets: Boolean,
)

fun List<MigratingEntry>.migrationMergeImpactSummary(): MigrationMergeImpactSummary {
    val pairs = mapNotNull { item ->
        val result = item.searchResult.value as? MigratingEntry.SearchResult.Success ?: return@mapNotNull null
        item.mergeContext to result.mergeContext
    }
    return MigrationMergeImpactSummary(
        hasAnyMergedEntries = pairs.any { (source, target) -> source != null || target != null },
        hasMergedRoots = pairs.any { (source, _) -> source?.isRoot == true },
        hasMergedMembers = pairs.any { (source, _) -> source?.isRoot == false },
        hasSameGroupTargets = pairs.any { (source, target) ->
            source != null && target?.rootEntryId == source.rootEntryId
        },
        hasOtherGroupTargets = pairs.any { (source, target) ->
            source != null && target != null && target.rootEntryId != source.rootEntryId
        },
        hasStandaloneSourcesWithMergedTargets = pairs.any { (source, target) -> source == null && target != null },
    )
}
