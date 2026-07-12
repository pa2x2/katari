package tachiyomi.domain.entry.interactor

import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository

class GetEntryById(
    private val entryRepository: EntryRepository,
) {

    suspend fun await(id: Long): Entry? {
        return entryRepository.getEntryById(id)
    }
}
