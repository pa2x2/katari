package tachiyomi.domain.entry.interactor

import dev.zacsweers.metro.Inject
import tachiyomi.domain.entry.repository.EntryRepository

@Inject
class SetEntryViewerFlags(
    private val entryRepository: EntryRepository,
) {

    suspend fun await(entryId: Long, viewerFlags: Long): Boolean {
        return entryRepository.setViewerFlags(entryId, viewerFlags)
    }
}
