package tachiyomi.domain.entry.interactor

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository

class GetEntry(
    private val entryRepository: EntryRepository,
) {

    suspend fun await(id: Long): Entry? {
        return try {
            entryRepository.getEntryById(id)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            null
        }
    }

    suspend fun subscribe(id: Long): Flow<Entry> {
        return entryRepository.getEntryByIdAsFlow(id)
    }

    suspend fun awaitNonFavoriteIds(entryIds: List<Long>): List<Long> {
        return entryRepository.getNonFavoriteIds(entryIds)
    }

    fun subscribe(url: String, sourceId: Long, type: EntryType): Flow<Entry?> {
        return entryRepository.getEntryByUrlAndSourceIdAsFlow(url, sourceId, type)
    }

    fun subscribe(url: String, sourceId: Long, type: EntryType, profileId: Long): Flow<Entry?> {
        return entryRepository.getEntryByUrlAndSourceIdAsFlow(url, sourceId, type, profileId)
    }
}
