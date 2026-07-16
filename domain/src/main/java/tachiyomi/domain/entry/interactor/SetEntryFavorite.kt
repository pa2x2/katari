package tachiyomi.domain.entry.interactor

import tachiyomi.domain.entry.repository.EntryRepository
import java.time.Instant

class SetEntryFavorite(
    private val entryRepository: EntryRepository,
    private val now: () -> Long = { Instant.now().toEpochMilli() },
) {

    suspend fun await(entryId: Long, favorite: Boolean): Boolean {
        val entry = entryRepository.getEntryById(entryId) ?: return false
        val dateAdded = if (favorite) now() else 0L
        return entryRepository.update(
            entry.copy(
                favorite = favorite,
                dateAdded = dateAdded,
            ),
        )
    }
}
