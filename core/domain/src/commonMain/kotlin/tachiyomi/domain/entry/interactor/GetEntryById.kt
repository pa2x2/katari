package tachiyomi.domain.entry.interactor

import dev.zacsweers.metro.Inject
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository

@Inject
class GetEntryById(
    private val entryRepository: EntryRepository,
) {

    suspend fun await(id: Long): Entry? {
        return entryRepository.getEntryById(id)
    }
}
