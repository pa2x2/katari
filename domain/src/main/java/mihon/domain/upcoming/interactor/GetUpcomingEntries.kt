package mihon.domain.upcoming.interactor

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

    suspend fun subscribe(
        profileId: Long,
        excludedCategories: List<Long>,
        includedCategories: List<Long>,
        hiddenSources: Set<Long>,
    ): Flow<List<Entry>> {
        return entryRepository.getUpcomingEntries(
            profileId = profileId,
            statuses = includedStatuses,
            types = EntryType.entries.toSet(),
            excludedCategories = excludedCategories,
            includedCategories = includedCategories,
        ).map { entries ->
            entries.filterNot { it.source in hiddenSources }
        }
    }
}
