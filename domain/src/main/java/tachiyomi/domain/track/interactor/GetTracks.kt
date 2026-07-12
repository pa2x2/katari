package tachiyomi.domain.track.interactor

import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.track.model.EntryTrack
import tachiyomi.domain.track.repository.TrackRepository

class GetTracks(
    private val trackRepository: TrackRepository,
) {

    suspend fun awaitOne(id: Long): EntryTrack? {
        return try {
            trackRepository.getTrackById(id)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            null
        }
    }

    suspend fun await(entryId: Long): List<EntryTrack> {
        return try {
            trackRepository.getTracksByEntryId(entryId)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }

    fun subscribe(entryId: Long): Flow<List<EntryTrack>> {
        return trackRepository.getTracksByEntryIdAsFlow(entryId)
    }
}
