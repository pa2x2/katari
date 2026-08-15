package tachiyomi.domain.entry.interactor

import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository
import kotlin.time.Clock

class UpdateEntry(
    private val entryRepository: EntryRepository,
) {

    suspend fun await(entryUpdate: Entry): Boolean {
        return entryRepository.update(entryUpdate)
    }

    suspend fun awaitAll(entryUpdates: List<Entry>): Boolean {
        var result = true
        entryUpdates.forEach {
            result = entryRepository.update(it) && result
        }
        return result
    }

    suspend fun awaitUpdateCoverLastModified(entryId: Long): Boolean {
        val entry = entryRepository.getEntryById(entryId) ?: return false
        return entryRepository.update(entry.copy(coverLastModified = Clock.System.now().toEpochMilliseconds()))
    }
}
