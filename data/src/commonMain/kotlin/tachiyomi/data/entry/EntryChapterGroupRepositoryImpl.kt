package tachiyomi.data.entry

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.entry.model.EntryChapterGroup
import tachiyomi.domain.entry.repository.EntryChapterGroupRepository

@Inject
@SingleIn(AppScope::class)
class EntryChapterGroupRepositoryImpl(
    private val handler: DatabaseHandler,
) : EntryChapterGroupRepository {

    override fun subscribe(entryIds: List<Long>): Flow<List<EntryChapterGroup>> {
        return handler.subscribeToList { chapter_groupsQueries.getByEntryIds(entryIds, ::mapGroup) }
    }

    override suspend fun getByEntryId(entryId: Long): List<EntryChapterGroup> {
        return handler.awaitList { chapter_groupsQueries.getByEntryId(entryId, ::mapGroup) }
    }

    override suspend fun getByEntryIds(entryIds: List<Long>): List<EntryChapterGroup> {
        return handler.awaitList { chapter_groupsQueries.getByEntryIds(entryIds, ::mapGroup) }
    }

    override suspend fun replace(entryId: Long, groups: List<EntryChapterGroup>) {
        handler.await(inTransaction = true) {
            chapter_groupsQueries.deleteByEntryId(entryId)
            groups.forEach { group ->
                chapter_groupsQueries.insert(
                    entryId = entryId,
                    groupKey = group.key,
                    title = group.title,
                    position = group.position.toLong(),
                )
            }
        }
    }

    private fun mapGroup(entryId: Long, groupKey: String, title: String, position: Long): EntryChapterGroup {
        return EntryChapterGroup(entryId = entryId, key = groupKey, title = title, position = position.toInt())
    }
}
