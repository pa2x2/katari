package mihon.entry.interactions.host.tracking

import mihon.entry.interactions.EntryTrackingMutation
import mihon.entry.interactions.EntryTrackingSearchCandidate
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.track.model.EntryTrack

interface EntryTrackingOperationHost {
    suspend fun refresh(entryId: Long, serviceIds: Set<Long>): EntryTrackingHostRefreshResult

    suspend fun search(serviceId: Long, query: String): List<EntryTrackingSearchCandidate>

    suspend fun register(
        serviceId: Long,
        candidate: EntryTrackingSearchCandidate,
        entryId: Long,
        private: Boolean,
    ): EntryTrack?

    suspend fun registerAutomatically(serviceId: Long, entry: Entry): EntryTrack?

    suspend fun mutate(serviceId: Long, track: EntryTrack, mutation: EntryTrackingMutation)

    suspend fun unregister(entryId: Long, serviceId: Long)

    suspend fun deleteRemote(serviceId: Long, track: EntryTrack)
}

data class EntryTrackingHostRefreshFailure(
    val serviceId: Long,
    val serviceName: String,
    val cause: Throwable,
)

data class EntryTrackingHostRefreshResult(
    val refreshedTracks: List<EntryTrack>,
    val failures: List<EntryTrackingHostRefreshFailure>,
)
