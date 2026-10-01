package tachiyomi.domain.entry.interactor

import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository

class NetworkToLocalEntry(
    private val entryRepository: EntryRepository,
) {

    suspend operator fun invoke(entry: Entry): Entry {
        return entryRepository.insertOrUpdate(entry)
    }

    suspend operator fun invoke(entry: Entry, profileId: Long): Entry {
        return entryRepository.insertOrUpdate(entry, profileId)
    }

    /**
     * Persists a whole page of network results in one transaction, so entry observers are
     * notified once per page instead of once per entry.
     */
    suspend operator fun invoke(entries: List<Entry>): List<Entry> {
        return entryRepository.insertOrUpdateBatch(entries)
    }

    suspend operator fun invoke(entries: List<Entry>, profileId: Long): List<Entry> {
        return entryRepository.insertOrUpdateBatch(entries, profileId)
    }
}
