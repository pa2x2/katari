package tachiyomi.domain.entry.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.service.EntryChildOwnershipResolutionPort
import tachiyomi.domain.entry.service.mergedForDisplay

class GetEntryWithChapters(
    private val entryChapterRepository: EntryChapterRepository,
    private val childOwnership: EntryChildOwnershipResolutionPort,
) {

    fun subscribe(
        entry: Entry,
        bypassMerge: Boolean = false,
    ): Flow<Pair<Entry, List<EntryChapter>>> {
        val owners = if (bypassMerge) {
            flowOf(listOf(entry))
        } else {
            childOwnership.observeChildOwnership(entry.profileId, entry.id)
                .map { resolution -> resolution.orderedOwners.ifEmpty { listOf(entry) } }
        }
        return owners.flatMapLatest { orderedOwners ->
            combine(
                orderedOwners.map { owner ->
                    entryChapterRepository.getChaptersByEntryId(owner.id)
                },
            ) { childLists ->
                entry to childLists.asIterable().mergedForDisplay(entry)
            }
        }
    }

    suspend fun awaitChapters(
        entry: Entry,
        bypassMerge: Boolean = false,
        applyScanlatorFilter: Boolean = false,
    ): List<EntryChapter> {
        val orderedOwners = if (bypassMerge) {
            listOf(entry)
        } else {
            childOwnership.resolveChildOwnership(entry.profileId, entry.id).orderedOwners.ifEmpty { listOf(entry) }
        }
        return orderedOwners
            .map { owner ->
                entryChapterRepository.getChaptersByEntryIdAwait(owner.id, applyScanlatorFilter)
            }
            .mergedForDisplay(entry)
    }
}
