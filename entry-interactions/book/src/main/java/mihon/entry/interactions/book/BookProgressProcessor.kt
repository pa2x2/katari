package mihon.entry.interactions.book

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryProgressProcessor
import mihon.entry.interactions.EntryProgressResourceMapping
import mihon.entry.interactions.EntryProgressSnapshot
import mihon.entry.interactions.EntryProgressStateSnapshot
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository

internal class BookProgressProcessor(
    private val entryProgressRepository: EntryProgressRepository,
    private val entryChapterRepository: EntryChapterRepository,
) : EntryProgressProcessor {
    override val type = EntryType.BOOK

    override suspend fun snapshot(entry: Entry): EntryProgressSnapshot {
        entry.requireBook()
        return EntryProgressSnapshot(
            states = entryProgressRepository.getByEntryId(entry.id).map { state ->
                state.toSnapshot(
                    sourceChildKey = state.chapterId
                        ?.let { entryChapterRepository.getChapterById(it) }
                        ?.url,
                )
            },
        )
    }

    override suspend fun restore(entry: Entry, snapshot: EntryProgressSnapshot) {
        entry.requireBook()
        snapshot.states.forEach { state ->
            val chapterId = state.sourceChildKey
                ?.let { entryChapterRepository.getChapterByUrlAndEntryId(it, entry.id) }
                ?.id
            entryProgressRepository.mergeAndSyncChild(state.toDomainState(entry.id, chapterId))
        }
    }

    override suspend fun copy(
        sourceEntry: Entry,
        targetEntry: Entry,
        resourceMappings: List<EntryProgressResourceMapping>,
    ) {
        sourceEntry.requireBook()
        targetEntry.requireBook()
        val sourceStates = entryProgressRepository.getByEntryId(sourceEntry.id)
            .associateBy { it.contentKey to it.resourceKey }

        resourceMappings.forEach { mapping ->
            val source = sourceStates[mapping.sourceContentKey to mapping.sourceResourceKey] ?: return@forEach
            entryProgressRepository.mergeAndSyncChild(
                source.copy(
                    entryId = targetEntry.id,
                    chapterId = mapping.targetChapterId,
                    contentKey = mapping.targetContentKey,
                    resourceKey = mapping.targetResourceKey,
                ),
            )
        }
    }
}

private fun EntryProgressState.toSnapshot(sourceChildKey: String?): EntryProgressStateSnapshot {
    return EntryProgressStateSnapshot(
        contentKey = contentKey,
        resourceKey = resourceKey,
        sourceChildKey = sourceChildKey,
        resourceRevision = resourceRevision,
        locator = locator,
        completed = completed,
        locatorUpdatedAt = locatorUpdatedAt,
        completionUpdatedAt = completionUpdatedAt,
    )
}

private fun EntryProgressStateSnapshot.toDomainState(entryId: Long, chapterId: Long?): EntryProgressState {
    return EntryProgressState(
        entryId = entryId,
        chapterId = chapterId,
        contentKey = contentKey,
        resourceKey = resourceKey,
        resourceRevision = resourceRevision,
        locator = locator,
        completed = completed,
        locatorUpdatedAt = locatorUpdatedAt,
        completionUpdatedAt = completionUpdatedAt,
    )
}
