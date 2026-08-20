package tachiyomi.domain.entry.interactor

import tachiyomi.domain.entry.repository.EntryRepository

class SetLibraryPinned(
    private val entryRepository: EntryRepository,
) {
    suspend fun await(profileId: Long, entryIds: List<Long>, libraryPinned: Boolean) {
        entryRepository.setLibraryPinned(profileId, entryIds, libraryPinned)
    }
}
