package mihon.entry.interactions.host.tracking

import kotlinx.coroutines.flow.Flow

interface EntryTrackingCollectionHost {
    fun observeCollection(): Flow<EntryTrackingHostCollectionSnapshot>
}

data class EntryTrackingHostCollectionSnapshot(
    val services: List<EntryTrackingHostService>,
    val entries: Map<Long, List<EntryTrackingHostCollectionTrack>>,
)

data class EntryTrackingHostCollectionTrack(
    val serviceId: Long,
    val normalizedScore: Double,
    val isScored: Boolean,
)
