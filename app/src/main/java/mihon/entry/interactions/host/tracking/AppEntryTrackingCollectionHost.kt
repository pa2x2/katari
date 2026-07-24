package mihon.entry.interactions.host.tracking

import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import tachiyomi.domain.track.interactor.GetTracksPerEntry

internal class AppEntryTrackingCollectionHost(
    private val trackerManager: TrackerManager,
    private val getTracksPerEntry: GetTracksPerEntry,
) : EntryTrackingCollectionHost {
    override fun observeCollection(): Flow<EntryTrackingHostCollectionSnapshot> {
        return combine(
            getTracksPerEntry.subscribe(),
            trackerManager.loggedInTrackersFlow(),
        ) { tracksByEntry, loggedInTrackers ->
            val servicesById = loggedInTrackers.associateBy(Tracker::id)
            EntryTrackingHostCollectionSnapshot(
                services = loggedInTrackers.map(Tracker::toHostService),
                entries = tracksByEntry.mapValues { (_, tracks) ->
                    tracks.mapNotNull { track ->
                        servicesById[track.trackerId]?.let { service ->
                            EntryTrackingHostCollectionTrack(
                                serviceId = service.id,
                                normalizedScore = service.get10PointScore(track),
                                isScored = track.score > 0.0,
                            )
                        }
                    }
                },
            )
        }
    }
}
