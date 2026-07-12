package tachiyomi.domain.entry.interactor

import tachiyomi.domain.entry.repository.EntryRepository

class SetEntryViewerFlags(
    private val entryRepository: EntryRepository,
) {

    suspend fun await(entryId: Long, viewerFlags: Long): Boolean {
        return entryRepository.setViewerFlags(entryId, viewerFlags)
    }
}
