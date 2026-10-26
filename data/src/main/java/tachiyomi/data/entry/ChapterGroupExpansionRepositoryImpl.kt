package tachiyomi.data.entry

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.entry.model.ChapterGroupId
import tachiyomi.domain.entry.repository.ChapterGroupExpansionRepository

@Inject
@SingleIn(AppScope::class)
class ChapterGroupExpansionRepositoryImpl(
    private val handler: DatabaseHandler,
) : ChapterGroupExpansionRepository {

    override fun subscribeExpanded(entryIds: Collection<Long>): Flow<Set<ChapterGroupId>> {
        return handler.subscribeToList { expanded_chapter_groupsQueries.getByEntryIds(entryIds, ::mapGroupId) }
            .map(List<ChapterGroupId>::toSet)
    }

    override suspend fun getExpanded(entryIds: Collection<Long>): Set<ChapterGroupId> {
        return handler.awaitList { expanded_chapter_groupsQueries.getByEntryIds(entryIds, ::mapGroupId) }.toSet()
    }

    override suspend fun setExpanded(group: ChapterGroupId, expanded: Boolean) {
        val groupKey = group.groupKey.orEmpty()
        handler.await {
            if (expanded) {
                expanded_chapter_groupsQueries.insert(group.entryId, groupKey)
            } else {
                expanded_chapter_groupsQueries.delete(group.entryId, groupKey)
            }
        }
    }

    // The table stores the ungrouped chapters' section under the empty key
    private fun mapGroupId(entryId: Long, groupKey: String): ChapterGroupId =
        ChapterGroupId(entryId, groupKey.ifEmpty { null })
}
