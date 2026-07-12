package tachiyomi.domain.entry.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryMerge
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.MergedEntryRepository
import tachiyomi.domain.entry.service.sortedForMergedDisplay

class GetEntryWithChapters(
    private val entryRepository: EntryRepository,
    private val entryChapterRepository: EntryChapterRepository,
    private val mergedEntryRepository: MergedEntryRepository,
) {

    suspend fun subscribe(
        id: Long,
        bypassMerge: Boolean = false,
    ): Flow<Pair<Entry, List<EntryChapter>>> {
        return combine(
            entryRepository.getEntryByIdAsFlow(id),
            mergedEntryRepository.subscribeGroupByEntryId(id),
        ) { entry, merges ->
            entry to if (bypassMerge) emptyList() else merges
        }
            .flatMapLatest { (anime, merges) ->
                if (merges.isEmpty()) {
                    entryChapterRepository.getChaptersByEntryId(id)
                        .map { anime to it }
                } else {
                    mergedChaptersAsFlow(anime, merges)
                        .map { anime to it }
                }
            }
    }

    suspend fun awaitEntry(id: Long): Entry {
        return entryRepository.getEntryById(id) ?: Entry.create()
    }

    suspend fun awaitChapters(
        id: Long,
        bypassMerge: Boolean = false,
    ): List<EntryChapter> {
        val merges = if (bypassMerge) {
            emptyList()
        } else {
            mergedEntryRepository.getGroupByEntryId(id)
        }

        return if (merges.isEmpty()) {
            entryChapterRepository.getChaptersByEntryIdAwait(id)
        } else {
            val anime = awaitEntry(id)
            mergedChapters(anime, merges)
        }
    }

    private suspend fun mergedChapters(
        entry: Entry,
        merges: List<EntryMerge>,
    ): List<EntryChapter> {
        return mergeChapters(
            entry = entry,
            episodeLists = merges.sortedBy { it.position }
                .map { merge -> entryChapterRepository.getChaptersByEntryIdAwait(merge.entryId) },
        )
    }

    private fun mergedChaptersAsFlow(
        entry: Entry,
        merges: List<EntryMerge>,
    ): Flow<List<EntryChapter>> {
        val orderedMerges = merges.sortedBy { it.position }
        return combine(
            orderedMerges.map { merge ->
                entryChapterRepository.getChaptersByEntryId(merge.entryId)
            },
        ) { episodeLists ->
            mergeChapters(entry, episodeLists.asIterable())
        }
    }

    private fun mergeChapters(
        entry: Entry,
        episodeLists: Iterable<List<EntryChapter>>,
    ): List<EntryChapter> {
        val mergedAnimeIds = episodeLists.mapNotNull { it.firstOrNull()?.entryId }
        return episodeLists.flatten()
            .sortedForMergedDisplay(entry, mergedAnimeIds)
    }
}
