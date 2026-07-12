package eu.kanade.domain.chapter.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.domain.entry.repository.EntryChapterRepository

class GetAvailableScanlators(
    private val entryChapterRepository: EntryChapterRepository,
) {

    private fun List<String>.cleanupAvailableScanlators(): Set<String> {
        return mapNotNull { it.ifBlank { null } }.toSet()
    }

    suspend fun await(entryId: Long): Set<String> {
        return entryChapterRepository.getScanlatorsByEntryId(entryId)
            .cleanupAvailableScanlators()
    }

    fun subscribe(entryId: Long): Flow<Set<String>> {
        return entryChapterRepository.getScanlatorsByEntryIdAsFlow(entryId)
            .map { it.cleanupAvailableScanlators() }
    }
}
