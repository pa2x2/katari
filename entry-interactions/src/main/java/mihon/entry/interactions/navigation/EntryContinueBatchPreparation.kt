package mihon.entry.interactions.navigation

import kotlinx.coroutines.flow.first
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.service.EntryChildOwnershipResolutionPort
import tachiyomi.domain.entry.service.mergedForDisplay

internal interface EntryContinueBatchPreparation {
    suspend fun prepare(entries: List<Entry>): Map<Long, EntryContinuePreparedInput>
}

internal data class EntryContinuePreparedInput(
    val chapters: List<EntryChapter>,
    val progressStates: List<EntryProgressState>,
)

internal class DefaultEntryContinueBatchPreparation(
    private val childOwnership: EntryChildOwnershipResolutionPort,
    private val chapterRepository: EntryChapterRepository,
    private val progressRepository: EntryProgressRepository,
) : EntryContinueBatchPreparation {
    override suspend fun prepare(entries: List<Entry>): Map<Long, EntryContinuePreparedInput> {
        return buildMap {
            entries.distinctBy(Entry::id)
                .groupBy(Entry::profileId)
                .forEach { (profileId, profileEntries) ->
                    putAll(prepareProfile(profileId, profileEntries))
                }
        }
    }

    private suspend fun prepareProfile(
        profileId: Long,
        entries: List<Entry>,
    ): Map<Long, EntryContinuePreparedInput> {
        val ownershipByEntryId = childOwnership.resolveChildOwnership(
            profileId = profileId,
            entryIds = entries.mapTo(mutableSetOf(), Entry::id),
        )
        val ownersByEntryId = entries.associate { entry ->
            entry.id to ownershipByEntryId[entry.id]?.orderedOwners.orEmpty().ifEmpty { listOf(entry) }
        }
        val ownerIds = ownersByEntryId.values.flatten().map(Entry::id).distinct()
        val chaptersByEntryId = chapterRepository.getChaptersByEntryIds(ownerIds).first()
            .groupBy(EntryChapter::entryId)
        val progressByEntryId = progressRepository.getByEntryIds(chaptersByEntryId.keys)
            .groupBy(EntryProgressState::entryId)

        return entries.associate { entry ->
            val chapters = ownersByEntryId.getValue(entry.id)
                .map { owner -> chaptersByEntryId[owner.id].orEmpty() }
                .mergedForDisplay(entry)
            val progressStates = chapters.map(EntryChapter::entryId)
                .distinct()
                .flatMap { ownerId -> progressByEntryId[ownerId].orEmpty() }
            entry.id to EntryContinuePreparedInput(chapters, progressStates)
        }
    }
}
