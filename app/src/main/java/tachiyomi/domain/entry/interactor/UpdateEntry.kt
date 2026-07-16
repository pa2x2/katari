package tachiyomi.domain.entry.interactor

import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.service.FetchInterval
import java.time.Instant
import java.time.ZonedDateTime

class UpdateEntry(
    private val entryRepository: EntryRepository,
    private val fetchInterval: FetchInterval,
) {

    suspend fun await(entryUpdate: Entry): Boolean {
        return entryRepository.update(entryUpdate)
    }

    suspend fun awaitAll(entryUpdates: List<Entry>): Boolean {
        var result = true
        entryUpdates.forEach {
            result = entryRepository.update(it) && result
        }
        return result
    }

    suspend fun awaitUpdateFetchInterval(
        entry: Entry,
        dateTime: ZonedDateTime = ZonedDateTime.now(),
        window: Pair<Long, Long> = fetchInterval.getWindow(dateTime),
    ): Boolean {
        val updated = fetchInterval.update(entry, dateTime, window)
        return entryRepository.update(updated)
    }

    suspend fun awaitUpdateLastUpdate(entryId: Long): Boolean {
        val entry = entryRepository.getEntryById(entryId) ?: return false
        return entryRepository.update(entry.copy(lastUpdate = Instant.now().toEpochMilli()))
    }

    suspend fun awaitUpdateCoverLastModified(entryId: Long): Boolean {
        val entry = entryRepository.getEntryById(entryId) ?: return false
        return entryRepository.update(entry.copy(coverLastModified = Instant.now().toEpochMilli()))
    }

    suspend fun awaitUpdateDisplayName(entryId: Long, displayName: String?): Boolean {
        return entryRepository.updateDisplayName(entryId, displayName)
    }
}
