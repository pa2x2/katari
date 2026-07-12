package tachiyomi.domain.entry.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.repository.EntryRepository

class SetEntryCategories(
    private val entryRepository: EntryRepository,
) {

    suspend fun await(entryId: Long, categoryIds: List<Long>) {
        try {
            entryRepository.setCategories(entryId, categoryIds)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }
}
