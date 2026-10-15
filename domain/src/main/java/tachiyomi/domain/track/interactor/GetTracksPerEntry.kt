package tachiyomi.domain.track.interactor

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.domain.track.model.EntryTrack
import tachiyomi.domain.track.repository.TrackRepository

@Inject
class GetTracksPerEntry(
    private val trackRepository: TrackRepository,
) {

    fun subscribe(): Flow<Map<Long, List<EntryTrack>>> {
        return trackRepository.getTracksAsFlow().map { tracks -> tracks.groupBy { it.entryId } }
    }
}
