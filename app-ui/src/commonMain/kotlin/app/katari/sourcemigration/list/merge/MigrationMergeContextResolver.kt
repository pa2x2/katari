package app.katari.sourcemigration.list.merge

import app.katari.entry.interactions.merge.EntryMergeLibraryGroupingFeature
import app.katari.sourcemigration.list.models.MigrationMergeContext
import dev.zacsweers.metro.Inject
import tachiyomi.domain.entry.repository.EntryRepository

@Inject
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
