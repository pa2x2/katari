package mihon.domain.upcoming.interactor

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryStatus
import tachiyomi.domain.entry.repository.EntryRepository

class GetUpcomingEntries(
    private val entryRepository: EntryRepository,
) {

    private val includedStatuses = setOf(
        EntryStatus.ONGOING.value,
        EntryStatus.PUBLISHING_FINISHED.value,
    )

    suspend fun subscribe(): Flow<List<Entry>> {
        return entryRepository.getUpcomingEntries(includedStatuses, EntryType.entries.toSet())
    }
}
