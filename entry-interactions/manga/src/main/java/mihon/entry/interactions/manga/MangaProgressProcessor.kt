package mihon.entry.interactions.manga

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryProgressProcessor
import mihon.entry.interactions.EntryProgressResourceMapping
import mihon.entry.interactions.EntryProgressSnapshot
import mihon.entry.interactions.EntryProgressStateSnapshot
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository

internal class MangaProgressProcessor(
    private val entryProgressRepository: EntryProgressRepository,
    private val entryChapterRepository: EntryChapterRepository,
) : EntryProgressProcessor {
    override val type: EntryType = EntryType.MANGA

    override suspend fun snapshot(entry: Entry): EntryProgressSnapshot {
        entry.requireManga()
        return EntryProgressSnapshot(
            states = entryProgressRepository.getByEntryId(entry.id).map(EntryProgressState::toSnapshot),
        )
    }

    override suspend fun restore(entry: Entry, snapshot: EntryProgressSnapshot) {
        entry.requireManga()
        snapshot.states.forEach { state ->
            val chapter = (state.sourceChildKey ?: state.resourceKey).let { childKey ->
                entryChapterRepository.getChapterByUrlAndEntryId(childKey, entry.id)
            }
            entryProgressRepository.mergeAndSyncChild(
                state.toDomainState(entryId = entry.id, chapterId = chapter?.id),
            )
        }
    }

    override suspend fun copy(
        sourceEntry: Entry,
        targetEntry: Entry,
        resourceMappings: List<EntryProgressResourceMapping>,
    ) {
        sourceEntry.requireManga()
        targetEntry.requireManga()
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

private fun EntryProgressState.toSnapshot(): EntryProgressStateSnapshot {
    return EntryProgressStateSnapshot(
        contentKey = contentKey,
        resourceKey = resourceKey,
        sourceChildKey = resourceKey,
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
