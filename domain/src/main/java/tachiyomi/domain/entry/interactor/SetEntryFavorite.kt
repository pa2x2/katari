package tachiyomi.domain.entry.interactor

import tachiyomi.domain.entry.repository.EntryRepository

class SetEntryFavorite(
    private val entryRepository: EntryRepository,
) {

    suspend fun await(entryId: Long, favorite: Boolean): Boolean {
        return entryRepository.setFavorite(entryId, favorite)
    }
}
