package eu.kanade.domain.track.interactor

import eu.kanade.domain.track.model.toDbTrack
import eu.kanade.domain.track.model.toDomainTrack
import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.interactor.InsertTrack
import tachiyomi.domain.track.model.EntryTrack

class RefreshTracks(
    private val getTracks: GetTracks,
    private val trackerManager: TrackerManager,
    private val insertTrack: InsertTrack,
) {

    /**
     * Fetches updated tracking data from all logged in trackers.
     *
     * @return Failed updates.
     */
    suspend fun await(entryId: Long, serviceIds: Set<Long>): TrackRefreshResult {
        return supervisorScope {
            val outcomes = getTracks.await(entryId)
                .map { it to trackerManager.get(it.trackerId) }
                .mapNotNull { (track, service) ->
                    service?.takeIf { it.id in serviceIds && it.isLoggedIn }?.let { track to it }
                }
                .map { (track, service) ->
                    async {
                        return@async try {
                            val updatedTrack = service.refresh(track.toDbTrack()).toDomainTrack()!!
                            insertTrack.await(updatedTrack)
                            TrackRefreshOutcome.Updated(updatedTrack)
                        } catch (e: Throwable) {
                            TrackRefreshOutcome.Failed(service, e)
                        }
                    }
                }
                .awaitAll()
            TrackRefreshResult(
                refreshedTracks = outcomes.filterIsInstance<TrackRefreshOutcome.Updated>().map { it.track },
                failures = outcomes.filterIsInstance<TrackRefreshOutcome.Failed>().map { it.tracker to it.cause },
            )
        }
    }
}

data class TrackRefreshResult(
    val refreshedTracks: List<EntryTrack>,
    val failures: List<Pair<BaseTracker, Throwable>>,
)

private sealed interface TrackRefreshOutcome {
    data class Updated(val track: EntryTrack) : TrackRefreshOutcome

    data class Failed(val tracker: BaseTracker, val cause: Throwable) : TrackRefreshOutcome
}
