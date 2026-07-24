package mihon.entry.interactions

import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryRepository

data class EntryDownloadOwner(
    val entry: Entry,
    val children: List<EntryChapter>,
)

/** Resolves selected children to their real, profile-scoped owner entries. */
class EntryDownloadOwnerResolver(
    private val entryRepository: EntryRepository,
) {
    suspend fun resolve(visibleEntry: Entry, children: List<EntryChapter>): List<EntryDownloadOwner> {
        return children.groupBy(EntryChapter::entryId).mapNotNull { (ownerId, ownerChildren) ->
            val owner = if (ownerId == visibleEntry.id) {
                visibleEntry
            } else {
                entryRepository.getEntryById(ownerId)
            }
            owner
                ?.takeIf { it.profileId == visibleEntry.profileId && it.type == visibleEntry.type }
                ?.let { EntryDownloadOwner(it, ownerChildren) }
        }
    }
}
