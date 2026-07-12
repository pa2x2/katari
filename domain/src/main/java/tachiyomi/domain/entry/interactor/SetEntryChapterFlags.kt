package tachiyomi.domain.entry.interactor

import tachiyomi.domain.entry.repository.EntryRepository

class SetEntryChapterFlags(
    private val entryRepository: EntryRepository,
) {

    suspend fun await(entryId: Long, flags: Long): Boolean {
        return entryRepository.setChapterFlags(entryId, flags)
    }
}
