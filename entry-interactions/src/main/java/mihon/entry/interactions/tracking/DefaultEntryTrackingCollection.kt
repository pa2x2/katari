package mihon.entry.interactions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import mihon.entry.interactions.host.tracking.EntryTrackingHost

internal class DefaultEntryTrackingCollection(
    private val host: EntryTrackingHost,
) : EntryTrackingCollection {
    override fun observeCollection(): Flow<EntryTrackingCollectionSnapshot> {
        return host.collection.observeCollection().map { snapshot ->
            EntryTrackingCollectionSnapshot(
                services = snapshot.services.map { it.toDescriptor() },
                scoreSupportedEntryTypes = snapshot.services
                    .flatMapTo(mutableSetOf()) { it.supportedEntryTypes },
                entries = snapshot.entries.mapValues { (_, tracks) ->
                    tracks.map { track ->
                        EntryTrackingCollectionTrack(
                            serviceId = EntryTrackingServiceId(track.serviceId),
                            normalizedScore = track.normalizedScore,
                            isScored = track.isScored,
                        )
                    }
                },
            )
        }
    }

    override suspend fun summarizeCollection(entryIds: Set<Long>): EntryTrackingCollectionSummary {
        val snapshot = observeCollection().first()
        val entries = entryIds.mapNotNull(snapshot.entries::get)
        val scoredEntryMeans = entries.mapNotNull { tracks ->
            tracks.filter(EntryTrackingCollectionTrack::isScored)
                .map(EntryTrackingCollectionTrack::normalizedScore)
                .takeIf(List<Double>::isNotEmpty)
                ?.average()
        }
        return EntryTrackingCollectionSummary(
            trackedEntryCount = entries.count(List<EntryTrackingCollectionTrack>::isNotEmpty),
            meanScore = scoredEntryMeans.average(),
            serviceCount = snapshot.services.size,
        )
    }
}
