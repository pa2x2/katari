package tachiyomi.domain.entry.interactor

import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository

class NetworkToLocalEntry(
    private val entryRepository: EntryRepository,
) {

    suspend operator fun invoke(entry: Entry): Entry {
        return invoke(listOf(entry)).single()
    }

    suspend operator fun invoke(entries: List<Entry>): List<Entry> {
        return entries.map {
            entryRepository.insertOrUpdate(it)
        }
    }
}
