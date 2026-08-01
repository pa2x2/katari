package mihon.feature.migration.list.merge

import mihon.entry.interactions.merge.EntryMergeLibraryGroupingFeature
import mihon.feature.migration.list.models.MigrationMergeContext
import tachiyomi.domain.entry.repository.EntryRepository

class MigrationMergeContextResolver(
    private val grouping: EntryMergeLibraryGroupingFeature,
    private val entryRepository: EntryRepository,
) {
    suspend fun resolve(profileId: Long): Map<Long, MigrationMergeContext> {
        return grouping.groupLibraryEntries(
            profileId = profileId,
            entries = entryRepository.getFavoritesByProfile(profileId),
        ).groups
            .filter { it.orderedEntries.size > 1 }
            .flatMap { group ->
                group.orderedEntries.map { entry ->
                    entry.id to MigrationMergeContext(
                        rootEntryId = group.visibleEntry.id,
                        rootTitle = group.visibleEntry.displayTitle,
                        memberCount = group.orderedEntries.size,
                        isRoot = entry.id == group.visibleEntry.id,
                    )
                }
            }
            .toMap()
    }
}
