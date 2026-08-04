package tachiyomi.domain.entry.interactor

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.service.FetchInterval
import kotlin.time.Clock

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
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        dateTime: LocalDateTime = Clock.System.now().toLocalDateTime(timeZone),
        window: Pair<Long, Long> = fetchInterval.getWindow(dateTime.date, timeZone),
    ): Boolean {
        val updated = fetchInterval.update(entry, dateTime, timeZone, window)
        return entryRepository.update(updated)
    }

    suspend fun awaitUpdateLastUpdate(entryId: Long): Boolean {
        val entry = entryRepository.getEntryById(entryId) ?: return false
        return entryRepository.update(entry.copy(lastUpdate = Clock.System.now().toEpochMilliseconds()))
    }

    suspend fun awaitUpdateCoverLastModified(entryId: Long): Boolean {
        val entry = entryRepository.getEntryById(entryId) ?: return false
        return entryRepository.update(entry.copy(coverLastModified = Clock.System.now().toEpochMilliseconds()))
    }

    suspend fun awaitUpdateDisplayName(entryId: Long, displayName: String?): Boolean {
        return entryRepository.updateDisplayName(entryId, displayName)
    }
}
