package tachiyomi.domain.track.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.track.repository.TrackRepository

class DeleteTrack(
    private val trackRepository: TrackRepository,
) {

    suspend fun await(entryId: Long, trackerId: Long) {
        try {
            trackRepository.delete(entryId, trackerId)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }
}
