package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow

interface EntryTrackingCollection {
    fun observeCollection(): Flow<EntryTrackingCollectionSnapshot>

    suspend fun summarizeCollection(entryIds: Set<Long>): EntryTrackingCollectionSummary
}

data class EntryTrackingCollectionSnapshot(
    val services: List<EntryTrackingServiceDescriptor>,
    val scoreSupportedEntryTypes: Set<EntryType>,
    val entries: Map<Long, List<EntryTrackingCollectionTrack>>,
)

data class EntryTrackingCollectionTrack(
    val serviceId: EntryTrackingServiceId,
    val normalizedScore: Double,
    val isScored: Boolean,
)

data class EntryTrackingCollectionSummary(
    val trackedEntryCount: Int,
    val meanScore: Double,
    val serviceCount: Int,
)
