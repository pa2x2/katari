package tachiyomi.domain.track.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.track.model.EntryTrack
import tachiyomi.domain.track.repository.TrackRepository

class InsertTrack(
    private val trackRepository: TrackRepository,
) {

    suspend fun await(track: EntryTrack) {
        try {
            trackRepository.insert(track)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }

    suspend fun awaitAll(tracks: List<EntryTrack>) {
        try {
            trackRepository.insertAll(tracks)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }
}
