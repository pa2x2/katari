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

    suspend fun prepare(
        entries: List<Entry>,
        seed: EntryContinueBatchSeed,
    ): Map<Long, EntryContinuePreparedInput> = prepare(entries)
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
        return prepareWithSeed(entries, seed = null)
    }

    override suspend fun prepare(
        entries: List<Entry>,
        seed: EntryContinueBatchSeed,
    ): Map<Long, EntryContinuePreparedInput> {
        return prepareWithSeed(entries, seed)
    }

    private suspend fun prepareWithSeed(
        entries: List<Entry>,
        seed: EntryContinueBatchSeed?,
    ): Map<Long, EntryContinuePreparedInput> {
        return buildMap {
            entries.distinctBy(Entry::id)
                .groupBy(Entry::profileId)
                .forEach { (profileId, profileEntries) ->
                    putAll(prepareProfile(profileId, profileEntries, seed))
                }
        }
    }

    private suspend fun prepareProfile(
        profileId: Long,
        entries: List<Entry>,
        seed: EntryContinueBatchSeed?,
    ): Map<Long, EntryContinuePreparedInput> {
        val ownershipByEntryId = childOwnership.resolveChildOwnership(
            profileId = profileId,
            entryIds = entries.mapTo(mutableSetOf(), Entry::id),
        )
        val ownersByEntryId = entries.associate { entry ->
            entry.id to ownershipByEntryId[entry.id]?.orderedOwners.orEmpty().ifEmpty { listOf(entry) }
        }
        val ownerIds = ownersByEntryId.values.flatten().map(Entry::id).distinct()
        val completeOwnerIds = seed?.completeOwnerIds.orEmpty()
        val missingOwnerIds = ownerIds.filterNot(completeOwnerIds::contains)
        val loadedChapters = chapterRepository.getChaptersByEntryIds(missingOwnerIds).first()
        val chaptersByEntryId = seed.orEmptyChaptersFor(ownerIds)
            .plus(loadedChapters)
            .groupBy(EntryChapter::entryId)
        val loadedProgressEntryIds = missingOwnerIds.filterTo(mutableSetOf()) { it in chaptersByEntryId }
        val progressByEntryId = seed.orEmptyProgressFor(ownerIds)
            .plus(progressRepository.getByEntryIds(loadedProgressEntryIds))
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

private fun EntryContinueBatchSeed?.orEmptyChaptersFor(ownerIds: List<Long>): List<EntryChapter> {
    if (this == null) return emptyList()
    val ownerIdSet = ownerIds.toSet()
    return chapters.filter { it.entryId in ownerIdSet }
}

private fun EntryContinueBatchSeed?.orEmptyProgressFor(ownerIds: List<Long>): List<EntryProgressState> {
    if (this == null) return emptyList()
    val ownerIdSet = ownerIds.toSet()
    return progressStates.filter { it.entryId in ownerIdSet }
}
