package tachiyomi.domain.entry.interactor

import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository

class UpdateEntryFromSource(
    private val entryRepository: EntryRepository,
) {

    suspend fun await(entry: Entry): Boolean {
        return entryRepository.updateFromSource(entry)
    }
}
