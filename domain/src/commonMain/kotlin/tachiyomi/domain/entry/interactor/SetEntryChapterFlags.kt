package tachiyomi.domain.entry.interactor

import dev.zacsweers.metro.Inject
import tachiyomi.domain.entry.repository.EntryRepository

@Inject
class SetEntryChapterFlags(
    private val entryRepository: EntryRepository,
) {

    suspend fun await(entryId: Long, flags: Long): Boolean {
        return entryRepository.setChapterFlags(entryId, flags)
    }
}
